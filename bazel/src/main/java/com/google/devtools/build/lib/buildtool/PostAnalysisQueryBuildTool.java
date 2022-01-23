// Copyright 2018 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.buildtool;

import com.google.devtools.build.lib.analysis.AnalysisResult;
import com.google.devtools.build.lib.analysis.ViewCreationFailedException;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.query2.NamedThreadSafeOutputFormatterCallback;
import com.google.devtools.build.lib.query2.PostAnalysisQueryEnvironment;
import com.google.devtools.build.lib.query2.PostAnalysisQueryEnvironment.TopLevelConfigurations;
import com.google.devtools.build.lib.query2.engine.QueryEvalResult;
import com.google.devtools.build.lib.query2.engine.QueryException;
import com.google.devtools.build.lib.query2.engine.QueryExpression;
import com.google.devtools.build.lib.query2.engine.QueryUtil;
import com.google.devtools.build.lib.query2.engine.QueryUtil.AggregateAllOutputFormatterCallback;
import com.google.devtools.build.lib.runtime.CommandEnvironment;
import com.google.devtools.build.lib.runtime.QueryRuntimeHelper;
import com.google.devtools.build.lib.runtime.QueryRuntimeHelper.QueryRuntimeHelperException;
import com.google.devtools.build.lib.server.FailureDetails.FailureDetail;
import com.google.devtools.build.lib.server.FailureDetails.Query;
import com.google.devtools.build.lib.server.FailureDetails.Query.Code;
import com.google.devtools.build.lib.skyframe.SkyframeExecutorWrappingWalkableGraph;
import com.google.devtools.build.lib.util.DetailedExitCode;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.WalkableGraph;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Version of {@link BuildTool} that handles all work for queries based on results from the analysis
 * phase.
 */
public abstract class PostAnalysisQueryBuildTool<T> extends BuildTool {

  private final QueryExpression queryExpression;

  public PostAnalysisQueryBuildTool(CommandEnvironment env, QueryExpression queryExpression) {
    super(env);
    this.queryExpression = queryExpression;
  }

  @Override
  protected void postProcessAnalysisResult(BuildRequest request, AnalysisResult analysisResult)
      throws InterruptedException, ViewCreationFailedException, ExitException {
    // TODO: b/71905538 - this query will operate over the graph as constructed by analysis, but
    // will also pick up any nodes that are in the graph from prior builds. This makes the results
    // not reproducible at the level of a single command. Either tolerate, or wipe the analysis
    // graph beforehand if this option is specified, or add another option to wipe if desired
    // (SkyframeExecutor#handleAnalysisInvalidatingChange should be sufficient).
    if (queryExpression != null) {
      if (!env.getSkyframeExecutor().tracksStateForIncrementality()) {
        throw new ExitException(
            DetailedExitCode.of(
                FailureDetail.newBuilder()
                    .setMessage(
                        "Queries based on analysis results are not allowed if incrementality state"
                            + " is not being kept")
                    .setQuery(Query.newBuilder().setCode(Code.ANALYSIS_QUERY_PREREQ_UNMET))
                    .build()));
      }
      try (QueryRuntimeHelper queryRuntimeHelper =
          env.getRuntime().getQueryRuntimeHelperFactory().create(env)) {
        doPostAnalysisQuery(
            request,
            analysisResult.getConfigurationCollection().getHostConfiguration(),
            new TopLevelConfigurations(analysisResult.getTopLevelTargetsWithConfigs()),
            env.getSkyframeExecutor().getTransitiveConfigurationKeys(),
            queryRuntimeHelper,
            queryExpression);
      } catch (QueryException e) {
        String errorMessage = "Error doing post analysis query";
        if (!request.getKeepGoing()) {
          throw new ViewCreationFailedException(errorMessage, e.getFailureDetail(), e);
        }
        env.getReporter().error(null, errorMessage, e);
      } catch (IOException e) {
        String errorMessage = "I/O error doing post analysis query";
        if (!request.getKeepGoing()) {
          FailureDetail failureDetail =
              FailureDetail.newBuilder()
                  .setMessage(errorMessage + ": " + e.getMessage())
                  .setQuery(Query.newBuilder().setCode(Code.OUTPUT_FORMATTER_IO_EXCEPTION))
                  .build();
          throw new ViewCreationFailedException(errorMessage, failureDetail, e);
        }
        env.getReporter().error(null, errorMessage, e);
      } catch (QueryRuntimeHelperException e) {
        throw new ExitException(DetailedExitCode.of(e.getFailureDetail()));
      }
    }
  }

  protected abstract PostAnalysisQueryEnvironment<T> getQueryEnvironment(
      BuildRequest request,
      BuildConfiguration hostConfiguration,
      TopLevelConfigurations topLevelConfigurations,
      Collection<SkyKey> transitiveConfigurationKeys,
      WalkableGraph walkableGraph)
      throws InterruptedException;

  private void doPostAnalysisQuery(
      BuildRequest request,
      BuildConfiguration hostConfiguration,
      TopLevelConfigurations topLevelConfigurations,
      Collection<SkyKey> transitiveConfigurationKeys,
      QueryRuntimeHelper queryRuntimeHelper,
      QueryExpression queryExpression)
      throws InterruptedException, QueryException, IOException, QueryRuntimeHelperException {
    WalkableGraph walkableGraph =
        SkyframeExecutorWrappingWalkableGraph.of(env.getSkyframeExecutor());

    PostAnalysisQueryEnvironment<T> postAnalysisQueryEnvironment =
        getQueryEnvironment(
            request,
            hostConfiguration,
            topLevelConfigurations,
            transitiveConfigurationKeys,
            walkableGraph);

    Iterable<NamedThreadSafeOutputFormatterCallback<T>> callbacks =
        postAnalysisQueryEnvironment.getDefaultOutputFormatters(
            postAnalysisQueryEnvironment.getAccessor(),
            env.getReporter(),
            queryRuntimeHelper.getOutputStreamForQueryOutput(),
            env.getSkyframeExecutor(),
            hostConfiguration,
            runtime.getRuleClassProvider().getTrimmingTransitionFactory(),
            env.getPackageManager());
    String outputFormat = postAnalysisQueryEnvironment.getOutputFormat();
    NamedThreadSafeOutputFormatterCallback<T> callback =
        NamedThreadSafeOutputFormatterCallback.selectCallback(outputFormat, callbacks);
    if (callback == null) {
      env.getReporter()
          .handle(
              Event.error(
                  String.format(
                      "Invalid output format '%s'. Valid values are: %s",
                      outputFormat,
                      NamedThreadSafeOutputFormatterCallback.callbackNames(callbacks))));
      return;
    }

    // A certain subset of output formatters support "streaming" results - the formatter is called
    // multiple times where each call has only a some of the full query results (see
    // StreamedOutputFormatter for details). cquery and aquery don't do this. But the reason is
    // subtle and hard to follow. Post-analysis output formatters inherit from Callback, which
    // declares "void process(Iterable<T> partialResult)". Its javadoc says that the subinterface
    // BatchCallback may stream partial results. But post-analysis callbacks don't inherit
    // BatchCallback!
    //
    // To protect against accidental feature regression (like implementing a callback that
    // accidentally inherits BatchCallback), we explicitly disable streaming here. The aggregating
    // callback collects the entire query's results, even if the query was evaluated in a streaming
    // manner. Note that streaming query evaluation is a distinct concept from streaming output
    // formatting. Once the complete query finishes, we replay the full results back to the original
    // callback. That way callback implementations can safely assume they're only called once and
    // the results for that call are indeed complete.
    AggregateAllOutputFormatterCallback<T, Set<T>> aggregateResultsCallback =
        QueryUtil.newOrderedAggregateAllOutputFormatterCallback(postAnalysisQueryEnvironment);
    QueryEvalResult result =
        postAnalysisQueryEnvironment.evaluateQuery(queryExpression, aggregateResultsCallback);
    if (result.isEmpty()) {
      env.getReporter().handle(Event.info("Empty query results"));
    }
    callback.start();
    callback.process(aggregateResultsCallback.getResult());
    callback.close(/*failFast=*/ !result.getSuccess());

    queryRuntimeHelper.afterQueryOutputIsWritten();
  }
}
