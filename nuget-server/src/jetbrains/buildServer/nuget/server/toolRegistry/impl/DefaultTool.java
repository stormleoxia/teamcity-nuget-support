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

package jetbrains.buildServer.nuget.server.toolRegistry.impl;

import jetbrains.buildServer.nuget.server.toolRegistry.NuGetInstalledTool;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Created 27.12.12 18:19
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class DefaultTool implements NuGetInstalledTool {
  private final NuGetInstalledTool myTool;

  public DefaultTool(@NotNull NuGetInstalledTool tool) {
    myTool = tool;
  }

  @NotNull
  public File getPath() {
    return myTool.getPath();
  }

  public boolean isDefaultTool() {
    return true;
  }

  @NotNull
  public String getId() {
    return myTool.getId();
  }

  @NotNull
  public String getVersion() {
    return myTool.getVersion();
  }
}
