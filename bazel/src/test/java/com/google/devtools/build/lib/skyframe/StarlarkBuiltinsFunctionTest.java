// Copyright 2020 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.skyframe;

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.build.skyframe.ErrorInfoSubjectFactory.assertThatErrorInfo;
import static com.google.devtools.build.skyframe.EvaluationResultSubjectFactory.assertThatEvaluationResult;

import com.google.devtools.build.lib.analysis.ConfiguredRuleClassProvider;
import com.google.devtools.build.lib.analysis.util.BuildViewTestCase;
import com.google.devtools.build.lib.analysis.util.MockRule;
import com.google.devtools.build.lib.skyframe.StarlarkBuiltinsFunction.BuiltinsFailedException;
import com.google.devtools.build.lib.skyframe.util.SkyframeExecutorTestUtils;
import com.google.devtools.build.lib.testutil.TestRuleClassProvider;
import com.google.devtools.build.skyframe.EvaluationResult;
import com.google.devtools.build.skyframe.SkyKey;
import net.starlark.java.eval.ClassObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link StarlarkBuiltinsFunction}. */
@RunWith(JUnit4.class)
public class StarlarkBuiltinsFunctionTest extends BuildViewTestCase {

  private static final MockRule OVERRIDABLE_RULE = () -> MockRule.define("overridable_rule");

  @Override
  protected ConfiguredRuleClassProvider createRuleClassProvider() {
    // Add a fake rule and top-level symbol to override.
    ConfiguredRuleClassProvider.Builder builder =
        new ConfiguredRuleClassProvider.Builder()
            .addRuleDefinition(OVERRIDABLE_RULE)
            .addStarlarkAccessibleTopLevels("overridable_symbol", "original_value")
            .addStarlarkAccessibleTopLevels("just_a_symbol", "another_value");
    TestRuleClassProvider.addStandardRules(builder);
    return builder.build();
  }

  // TODO(#11437): Add tests for predeclared env of BUILD (and WORKSPACE?) files, once
  // StarlarkBuiltinsFunction manages that functionality.

  /** Sets up exports.bzl with the given contents and evaluates the {@code @_builtins}. */
  private EvaluationResult<StarlarkBuiltinsValue> evalBuiltins(String... lines) throws Exception {
    scratch.file("tools/builtins_staging/exports.bzl", lines);
    setBuildLanguageOptions("--experimental_builtins_bzl_path=tools/builtins_staging");

    SkyKey key = StarlarkBuiltinsValue.key();
    return SkyframeExecutorTestUtils.evaluate(
        getSkyframeExecutor(), key, /*keepGoing=*/ false, reporter);
  }

  /**
   * Sets up exports.bzl with the given contents, evaluates the {@code @_builtins}, and asserts that
   * a BuiltinsFailedException is raised with the given message.
   */
  private void assertBuiltinsFailure(String message, String... lines) throws Exception {
    reporter.removeHandler(failFastHandler);
    EvaluationResult<StarlarkBuiltinsValue> result = evalBuiltins(lines);

    SkyKey key = StarlarkBuiltinsValue.key();
    assertThatEvaluationResult(result).hasError();
    assertThatErrorInfo(result.getError(key)).isNotTransient();
    Exception ex = result.getError(key).getException();
    assertThat(ex).isInstanceOf(BuiltinsFailedException.class);
    assertThat(ex).hasMessageThat().contains(message);
  }

  @Test
  public void successfulEvaluation() throws Exception {
    EvaluationResult<StarlarkBuiltinsValue> result =
        evalBuiltins(
            "exported_toplevels = {'overridable_symbol': 'new_value'}",
            "exported_rules = {'overridable_rule': 'new_rule'}",
            "exported_to_java = {'for_native_code': 'secret_sauce'}");

    SkyKey key = StarlarkBuiltinsValue.key();
    assertThatEvaluationResult(result).hasNoError();
    StarlarkBuiltinsValue value = result.get(key);

    // Universe symbols are omitted (they're added by the interpreter).
    assertThat(value.predeclaredForBuildBzl).doesNotContainKey("print");
    // General Bazel symbols are present.
    assertThat(value.predeclaredForBuildBzl).containsKey("rule");
    // Non-overridden rule-specific symbols are present.
    assertThat(value.predeclaredForBuildBzl).containsKey("just_a_symbol");
    // Overridden symbol.
    assertThat(value.predeclaredForBuildBzl).containsEntry("overridable_symbol", "new_value");
    // Overridden native field.
    Object nativeField =
        ((ClassObject) value.predeclaredForBuildBzl.get("native")).getValue("overridable_rule");
    assertThat(nativeField).isEqualTo("new_rule");
    // Stuff for native rules.
    assertThat(value.exportedToJava).containsExactly("for_native_code", "secret_sauce").inOrder();

    // Digest should be same as the exports file.
    byte[] exportsDigest =
        ((BzlLoadValue)
                SkyframeExecutorTestUtils.evaluate(
                        getSkyframeExecutor(),
                        StarlarkBuiltinsFunction.EXPORTS_ENTRYPOINT_KEY,
                        /*keepGoing=*/ false,
                        reporter)
                    .get(StarlarkBuiltinsFunction.EXPORTS_ENTRYPOINT_KEY))
            .getTransitiveDigest();
    assertThat(value.transitiveDigest).isEqualTo(exportsDigest);
  }

  @Test
  public void exportsDictMustExist() throws Exception {
    assertBuiltinsFailure(
        "Failed to apply declared builtins: expected a 'exported_rules' dictionary to be defined",
        //
        "exported_toplevels = {}",
        "# exported_rules missing",
        "exported_to_java = {}");
  }

  @Test
  public void exportsDictMustBeDict() throws Exception {
    assertBuiltinsFailure(
        "Failed to apply declared builtins: got NoneType for 'exported_rules dict', want dict",
        //
        "exported_toplevels = {}",
        "exported_rules = None",
        "exported_to_java = {}");
  }

  @Test
  public void exportsDictKeyMustBeString() throws Exception {
    assertBuiltinsFailure(
        "Failed to apply declared builtins: got dict<int, string> for 'exported_rules dict', want"
            + " dict<string, unknown>",
        //
        "exported_toplevels = {}",
        "exported_rules = {1: 'a'}",
        "exported_to_java = {}");
  }

  @Test
  public void cannotOverrideGeneralSymbol() throws Exception {
    assertBuiltinsFailure(
        "Failed to apply declared builtins: Cannot override native module field 'glob' with an"
            + " injected value",
        //
        "exported_toplevels = {}", //
        "exported_rules = {'glob': 'new_builtin'}",
        "exported_to_java = {}");
  }

  @Test
  public void parseErrorInExportsHandledGracefully() throws Exception {
    assertBuiltinsFailure(
        "Failed to load builtins sources: Extension 'exports.bzl' (internal) has errors",
        //
        "exported_toplevels = {}",
        "exported_rules = {}",
        "exported_to_java = {}",
        "asdf asdf  # <-- parse error");
  }

  @Test
  public void evalErrorInExportsHandledGracefully() throws Exception {
    assertBuiltinsFailure(
        "Failed to load builtins sources: Extension file 'exports.bzl' (internal) has errors",
        //
        "exported_toplevels = {}",
        "exported_rules = {}",
        "exported_to_java = {}",
        "1 // 0  # <-- dynamic error");
  }

  @Test
  public void builtinsBzlCannotAccessNative() throws Exception {
    assertBuiltinsFailure(
        "Extension 'exports.bzl' (internal) has errors",
        //
        "native.overridable_rule",
        "exported_toplevels = {}",
        "exported_rules = {}",
        "exported_to_java = {}");
    assertContainsEvent("name 'native' is not defined");
  }

  @Test
  public void builtinsBzlCannotAccessRuleSpecificSymbol() throws Exception {
    assertBuiltinsFailure(
        "Extension 'exports.bzl' (internal) has errors",
        //
        "overridable_symbol",
        "exported_toplevels = {}",
        "exported_rules = {}",
        "exported_to_java = {}");
    assertContainsEvent("name 'overridable_symbol' is not defined");
  }

  @Test
  public void builtinsBzlCanAccessInternal() throws Exception {
    EvaluationResult<StarlarkBuiltinsValue> result =
        evalBuiltins(
            "print(_internal)",
            "",
            "exported_toplevels = {}",
            "exported_rules = {}",
            "exported_to_java = {}");
    assertThatEvaluationResult(result).hasNoError();
    assertContainsEvent("<_internal module>");
  }

  @Test
  public void regularBzlCannotAccessInternal() throws Exception {
    scratch.file(
        "pkg/BUILD", //
        "load(':dummy.bzl', 'dummy_symbol')");
    scratch.file(
        "pkg/dummy.bzl", //
        "_internal",
        "dummy_symbol = None");

    reporter.removeHandler(failFastHandler);
    getConfiguredTarget("//pkg:BUILD");
    assertContainsEvent("name '_internal' is not defined");
  }
}
