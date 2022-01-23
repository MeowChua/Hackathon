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

package com.google.devtools.build.lib.analysis;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.analysis.platform.ConstraintValueInfo;
import javax.annotation.Nullable;

/**
 * Provider instance for the {@code target_compatible_with} attribute.
 *
 * <p>The presence of this provider is used to indicate that a target is incompatible with the
 * current platform. Any target that provides this will automatically be excluded from {@link
 * SkyframeAnalysisResult}'s list of configured targets.
 *
 * <p>This provider is able to keep track of _why_ the corresponding target is considered
 * incompatible. If the target is incompatible because the target platform didn't satisfy one of the
 * constraints in target_compatible_with, then all the relevant constraints are accessible via
 * {@code getConstraintsResponsibleForIncompatibility()}. On the other hand, if the corresponding
 * target is incompatible because one of its dependencies is incompatible, then all the incompatible
 * dependencies are available via {@code getTargetResponsibleForIncompatibility()}.
 */
@AutoValue
public abstract class IncompatiblePlatformProvider implements TransitiveInfoProvider {
  public static IncompatiblePlatformProvider incompatibleDueToTargets(
      ImmutableList<ConfiguredTarget> targetsResponsibleForIncompatibility) {
    Preconditions.checkNotNull(targetsResponsibleForIncompatibility);
    Preconditions.checkArgument(!targetsResponsibleForIncompatibility.isEmpty());
    return new AutoValue_IncompatiblePlatformProvider(targetsResponsibleForIncompatibility, null);
  }

  public static IncompatiblePlatformProvider incompatibleDueToConstraints(
      ImmutableList<ConstraintValueInfo> constraints) {
    Preconditions.checkNotNull(constraints);
    Preconditions.checkArgument(!constraints.isEmpty());
    return new AutoValue_IncompatiblePlatformProvider(null, constraints);
  }

  /**
   * Returns the incompatible dependencies that caused this provider to be present.
   *
   * <p>This may be null. If it is null, then {@code getConstraintsResponsibleForIncompatibility()}
   * is guaranteed to be non-null. It will have at least one element in it if it is not null.
   */
  @Nullable
  public abstract ImmutableList<ConfiguredTarget> targetsResponsibleForIncompatibility();

  /**
   * Returns the constraints that the target platform didn't satisfy.
   *
   * <p>This may be null. If it is null, then {@code getTargetsResponsibleForIncompatibility()} is
   * guaranteed to be non-null. It will have at least one element in it if it is not null.
   */
  @Nullable
  public abstract ImmutableList<ConstraintValueInfo> constraintsResponsibleForIncompatibility();
}
