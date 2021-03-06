/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.nuget.server.feed.server.index.impl.latest;

import jetbrains.buildServer.nuget.server.feed.server.index.impl.NuGetPackageBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Created 19.03.13 14:32
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class LatestVersionsCalculator implements LatestCalculator {
  private final LatestCalculator[] myCalcs = {
          new IsLatestCalculator(),
          new IsAbsoluteLatestCalculator()};

  public void updatePackage(@NotNull NuGetPackageBuilder newLatest) {
    for (LatestCalculator calc : myCalcs) {
      calc.updatePackage(newLatest);
    }
  }

  public void updateSelectedPackages() {
    for (LatestCalculator calc : myCalcs) {
      calc.updateSelectedPackages();
    }
  }
}
