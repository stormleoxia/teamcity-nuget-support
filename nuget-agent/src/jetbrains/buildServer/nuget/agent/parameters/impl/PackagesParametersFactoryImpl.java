/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package jetbrains.buildServer.nuget.agent.parameters.impl;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.BundledTool;
import jetbrains.buildServer.agent.BundledToolsRegistry;
import jetbrains.buildServer.nuget.agent.parameters.*;
import jetbrains.buildServer.nuget.common.PackagesUpdateMode;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static jetbrains.buildServer.nuget.common.PackagesConstants.*;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 07.07.11 18:09
 */
public class PackagesParametersFactoryImpl implements PackagesParametersFactory {
  private final BundledToolsRegistry myBundledTools;

  public PackagesParametersFactoryImpl(@NotNull final BundledToolsRegistry bundledTools) {
    myBundledTools = bundledTools;
  }

  @NotNull
  public NuGetFetchParameters loadNuGetFetchParameters(@NotNull final BuildRunnerContext context) throws RunBuildException {
    return new NuGetFetchParameters() {
      @NotNull
      public File getSolutionFile() throws RunBuildException {
        return getFile(context, SLN_PATH, "Visual Studio .sln file");
      }

      @NotNull
      public File getNuGetExeFile() throws RunBuildException {
        return getPathToNuGet(context);
      }

      @NotNull
      public Collection<String> getNuGetPackageSources() {
        return getMultilineParameter(context, NUGET_SOURCES);
      }

      public boolean getExcludeVersion() {
        return getBoolean(context, NUGET_EXCLUDE_VERSION);
      }
    };
  }

  private File getPathToNuGet(BuildRunnerContext context) throws RunBuildException {
    String path = getParameter(context, NUGET_PATH);
    if (StringUtil.isEmptyOrSpaces(path))
      throw new RunBuildException("Runner parameter '" + NUGET_PATH + "' was not found");

    if (path.startsWith("?")) {
      final BundledTool tool = myBundledTools.findTool(path.substring(1));
      if (tool != null) {
        path = new File(tool.getRootPath(), NUGET_TOOL_REL_PATH).getPath();
      }
    }

    File file = FileUtil.resolvePath(context.getBuild().getCheckoutDirectory(), path);
    if (!file.exists()) {
      throw new RunBuildException("Failed to find " + "nuget.exe" + " at " + file);
    }

    return file;
  }

  @NotNull
  private File getFile(@NotNull final BuildRunnerContext context,
                       @NotNull final String runnerParameter,
                       @NotNull final String fileName) throws RunBuildException {
    String path = getParameter(context, runnerParameter);
    if (StringUtil.isEmptyOrSpaces(path))
      throw new RunBuildException("Runner parameter '" + runnerParameter + "' was not found");

    File file = FileUtil.resolvePath(context.getBuild().getCheckoutDirectory(), path);
    if (!file.exists()) {
      throw new RunBuildException("Failed to find " + fileName + " at " + file);
    }

    return file;
  }


  private Collection<String> getMultilineParameter(BuildRunnerContext context, String nugetSources) {
    String sources = getParameter(context, nugetSources);
    if (sources == null) return Collections.emptyList();

    List<String> list = new ArrayList<String>();
    for (String _source : sources.split("[\\r\\n]+")) {
      final String source = _source.trim();
      if (!source.isEmpty()) {
        list.add(source);
      }
    }

    return Collections.unmodifiableList(list);
  }

  private boolean getBoolean(@NotNull BuildRunnerContext context, @NotNull String key) {
    return !StringUtil.isEmptyOrSpaces(getParameter(context, key));
  }

  private String getParameter(@NotNull BuildRunnerContext context, @NotNull String key) {
    return context.getRunnerParameters().get(key);
  }


  public PackagesInstallParameters loadInstallPackagesParameters(@NotNull final BuildRunnerContext context,
                                                                 @NotNull final NuGetFetchParameters nuget) throws RunBuildException {
    return new PackagesInstallParameters() {
      @NotNull
      public NuGetFetchParameters getNuGetParameters() {
        return nuget;
      }

      public boolean getExcludeVersion() {
        return getBoolean(context, NUGET_EXCLUDE_VERSION);
      }
    };
  }

  public PackagesUpdateParameters loadUpdatePackagesParameters(@NotNull final BuildRunnerContext context,
                                                               @NotNull final NuGetFetchParameters nuget) throws RunBuildException {
    if (!getBoolean(context, NUGET_UPDATE_PACKAGES)) return null;

    return new PackagesUpdateParameters() {
      @NotNull
      public NuGetFetchParameters getNuGetParameters() {
        return nuget;
      }

      @NotNull
      public PackagesUpdateMode getUpdateMode() {
        PackagesUpdateMode mode = PackagesUpdateMode.parse(getParameter(context, NUGET_UPDATE_MODE));
        return mode == null ? PackagesUpdateMode.FOR_SLN : mode;
      }

      public boolean getUseSafeUpdate() {
        return getBoolean(context, NUGET_UPDATE_PACKAGES_SAFE);
      }

      @NotNull
      public Collection<String> getPackagesToUpdate() {
        return getMultilineParameter(context, NUGET_UPDATE_PACKAGE_IDS);
      }
    };
  }

  @NotNull
  public NuGetPublishParameters loadPublishParameters(@NotNull final BuildRunnerContext context) throws RunBuildException {
    return new NuGetPublishParameters() {
      public String getPublishSource() throws RunBuildException {
        return getParameter(context, NUGET_PUBLISH_SOURCE);
      }

      @NotNull
      public String getApiKey() throws RunBuildException {
        return getParameter(context, NUGET_API_KEY);
      }

      @NotNull
      public Collection<String> getFiles() throws RunBuildException {
        return getMultilineParameter(context, NUGET_PUBLISH_FILES);
      }

      public boolean getCreateOnly() {
        return getBoolean(context, NUGET_PUBLISH_CREATE_ONLY);
      }

      @NotNull
      public File getNuGetExeFile() throws RunBuildException {
        return getPathToNuGet(context);
      }
    };
  }
}
