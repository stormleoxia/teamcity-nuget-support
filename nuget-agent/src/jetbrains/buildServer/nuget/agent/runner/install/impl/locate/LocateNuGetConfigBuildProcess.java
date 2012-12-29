/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package jetbrains.buildServer.nuget.agent.runner.install.impl.locate;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.nuget.agent.parameters.NuGetFetchParameters;
import jetbrains.buildServer.nuget.agent.runner.install.impl.RepositoryPathResolver;
import jetbrains.buildServer.nuget.agent.util.BuildProcessBase;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 07.07.11 16:03
 */
public class LocateNuGetConfigBuildProcess extends BuildProcessBase {
  private static final Logger LOG = Logger.getInstance(LocateNuGetConfigBuildProcess.class.getName());

  private final EventDispatcher<PackagesInstallerCallback> myDispatcher;
  private final NuGetFetchParameters myContext;
  private final BuildProgressLogger myLogger;
  private final RepositoryPathResolver myResolver;
  private final Collection<? extends PackagesConfigScanner> myScanners;

  public LocateNuGetConfigBuildProcess(@NotNull final NuGetFetchParameters context,
                                       @NotNull final BuildProgressLogger logger,
                                       @NotNull final RepositoryPathResolver resolver,
                                       @NotNull final List<PackagesConfigScanner> scanners) {
    myContext = context;
    myLogger = logger;
    myResolver = resolver;
    myDispatcher = EventDispatcher.create(PackagesInstallerCallback.class);
    myDispatcher.setErrorHandler(new EventDispatcher.ErrorHandler() {
      public void handle(Throwable e) {
        LOG.warn("Failed to process Installer Runner task. " + e.getMessage(), e);
        ExceptionUtil.rethrowAsRuntimeException(e);
      }
    });

    myScanners = scanners;
  }

  public void addInstallStageListener(@NotNull final PackagesInstallerCallback callback) {
    myDispatcher.addListener(callback);
  }

  @NotNull
  @Override
  protected BuildFinishedStatus waitForImpl() throws RunBuildException {
    myLogger.activityStarted("scan", "Searching for nuget.config files", "NuGet.Search");
    try {
      locatePackagesConfigFiles();
    } finally {
      myLogger.activityFinished("scan", "NuGet.Search");
    }
    return BuildFinishedStatus.FINISHED_SUCCESS;
  }

  private void locatePackagesConfigFiles() throws RunBuildException {
    final File sln = myContext.getSolutionFile();
    final File packages = myResolver.resolvePath(myLogger, sln);

    if (sln.isFile()) {
      LOG.debug("Found Visual Studio .sln file: " + sln);
      myDispatcher.getMulticaster().onSolutionFileFound(sln, packages);
    }

    myLogger.message("Found packages folder: " + packages);

    final Collection<File> files = new HashSet<File>();
    for (PackagesConfigScanner scanner : myScanners) {
      files.addAll(scanner.scanResourceConfig(myLogger, sln, packages));
    }

    for (Iterator<File> it = files.iterator(); it.hasNext(); ) {
      final File file = it.next();
      if (!file.isFile()) {
        myLogger.warning("Found packages.config file does not exist: " + file);
        it.remove();
      }
    }

    if (files.isEmpty()) {
      myLogger.warning("No packages.config files were found under solution. Nothing to install");
      return;
    }

    for (File file : files) {
      myDispatcher.getMulticaster().onPackagesConfigFound(file, packages);
    }
  }
}