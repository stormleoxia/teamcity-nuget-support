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

package jetbrains.buildServer.nuget.tests.integration;

import com.intellij.execution.configurations.GeneralCommandLine;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.SimpleCommandLineProcessRunner;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.nuget.agent.commands.NuGetActionFactory;
import jetbrains.buildServer.nuget.agent.commands.impl.*;
import jetbrains.buildServer.nuget.agent.dependencies.NuGetPackagesCollector;
import jetbrains.buildServer.nuget.agent.dependencies.PackageUsages;
import jetbrains.buildServer.nuget.agent.dependencies.impl.NuGetPackagesCollectorImpl;
import jetbrains.buildServer.nuget.agent.dependencies.impl.NuGetPackagesConfigParser;
import jetbrains.buildServer.nuget.agent.dependencies.impl.PackageUsagesImpl;
import jetbrains.buildServer.nuget.agent.parameters.NuGetFetchParameters;
import jetbrains.buildServer.nuget.agent.parameters.PackageSource;
import jetbrains.buildServer.nuget.agent.parameters.PackageSourceManager;
import jetbrains.buildServer.nuget.agent.parameters.PackagesParametersFactory;
import jetbrains.buildServer.nuget.agent.util.BuildProcessBase;
import jetbrains.buildServer.nuget.agent.util.CommandlineBuildProcessFactory;
import jetbrains.buildServer.nuget.common.PackageInfoLoader;
import jetbrains.buildServer.nuget.common.exec.NuGetTeamCityProvider;
import jetbrains.buildServer.nuget.tests.util.BuildProcessTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

import java.io.File;
import java.util.*;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 22.07.11 1:26
 */
public class IntegrationTestBase extends BuildProcessTestCase {
  private StringBuilder myCommandsOutput;
  protected File myRoot;
  protected Mockery m;
  protected AgentRunningBuild myBuild;
  protected BuildRunnerContext myContext;
  protected BuildProgressLogger myLogger;
  protected PackagesParametersFactory myParametersFactory;
  protected NuGetFetchParameters myFetchParameters;
  protected NuGetPackagesCollector myCollector;
  protected NuGetActionFactory myActionFactory;
  private BuildProcess myMockProcess;
  protected BuildParametersMap myBuildParametersMap;
  protected CommandlineBuildProcessFactory myExecutor;
  protected NuGetTeamCityProvider myNuGetTeamCityProvider;
  protected String cmd;
  protected List<PackageSource> myGlobalSources;

  @NotNull
  protected String getCommandsOutput() {
    return myCommandsOutput.toString();
  }


  public static final String NUGET_VERSIONS = "nuget_versions";
  public static final String NUGET_VERSIONS_15p = "nuget_versions_15p";
  public static final String NUGET_VERSIONS_16p = "nuget_versions_16p";
  public static final String NUGET_VERSIONS_17p = "nuget_versions_17p";
  public static final String NUGET_VERSIONS_18p = "nuget_versions_18p";
  public static final String NUGET_VERSIONS_20p = "nuget_versions_20p";
  public static final String NUGET_VERSIONS_27p = "nuget_versions_27p";
  public static final String NUGET_VERSIONS_28p = "nuget_versions_28p";

  @NotNull
  private Object[][] versionsFrom(@NotNull final NuGet lowerBound) {
    final NuGet[] values =
//            new NuGet[]{NuGet.NuGet_2_7};
            NuGet.values();

    final List<Object[]> data = new ArrayList<Object[]>();
    for (NuGet value : values) {
      if (value.major < lowerBound.major) continue;
      if (value.major == lowerBound.major && value.minor < lowerBound.minor) continue;
      data.add(new Object[]{value});
    }
    return data.toArray(new Object[data.size()][]);
  }

  @DataProvider(name = NUGET_VERSIONS)
  public Object[][] dataProviderNuGetVersions() {
    return versionsFrom(NuGet.NuGet_1_6);
  }

  @DataProvider(name = NUGET_VERSIONS_15p)
  public Object[][] dataProviderNuGetVersions15p() {
    return versionsFrom(NuGet.NuGet_1_6);
  }

  @DataProvider(name = NUGET_VERSIONS_16p)
  public Object[][] dataProviderNuGetVersions16p() {
    return versionsFrom(NuGet.NuGet_1_6);
  }

  @DataProvider(name = NUGET_VERSIONS_17p)
  public Object[][] dataProviderNuGetVersions17p() {
    return versionsFrom(NuGet.NuGet_1_7);
  }

  @DataProvider(name = NUGET_VERSIONS_18p)
  public Object[][] dataProviderNuGetVersions18p() {
    return versionsFrom(NuGet.NuGet_1_8);
  }

  @DataProvider(name = NUGET_VERSIONS_20p)
  public Object[][] dataProviderNuGetVersions20p() {
    return versionsFrom(NuGet.NuGet_2_0);
  }

  @DataProvider(name = NUGET_VERSIONS_27p)
  public Object[][] dataProviderNuGetVersions27p() {
    return versionsFrom(NuGet.NuGet_2_7);
  }

  @DataProvider(name = NUGET_VERSIONS_28p)
  public Object[][] dataProviderNuGetVersions28p() {
    return versionsFrom(NuGet.NuGet_2_8);
  }

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myGlobalSources = new ArrayList<PackageSource>();
    myCommandsOutput = new StringBuilder();
    myRoot = createTempDir();
    m = new Mockery();
    myBuild = m.mock(AgentRunningBuild.class);
    myContext = m.mock(BuildRunnerContext.class);
    myLogger = m.mock(BuildProgressLogger.class);
    myParametersFactory = m.mock(PackagesParametersFactory.class);
    myMockProcess = m.mock(BuildProcess.class);
    myFetchParameters = m.mock(NuGetFetchParameters.class);
    myBuildParametersMap = m.mock(BuildParametersMap.class);
    myNuGetTeamCityProvider = m.mock(NuGetTeamCityProvider.class);

    cmd = System.getenv("ComSpec");
    final PackageSourceManager psm = m.mock(PackageSourceManager.class);

    final Map<String, String> configParameters = new TreeMap<String, String>();

    m.checking(new Expectations(){{
      allowing(myContext).getBuildParameters(); will(returnValue(myBuildParametersMap));
      allowing(myContext).getConfigParameters(); will(returnValue(Collections.emptyMap()));
      allowing(myBuildParametersMap).getEnvironmentVariables(); will(returnValue(Collections.singletonMap("ComSpec", cmd)));

      allowing(myContext).getBuild(); will(returnValue(myBuild));
      allowing(myBuild).getBuildLogger();  will(returnValue(myLogger));
      allowing(myBuild).getCheckoutDirectory();  will(returnValue(myRoot));
      allowing(myBuild).getAgentTempDirectory(); will(returnValue(createTempDir()));

      allowing(myMockProcess).start();
      allowing(myMockProcess).waitFor();
      will(returnValue(BuildFinishedStatus.FINISHED_SUCCESS));

      allowing(myLogger).message(with(any(String.class)));

      allowing(myBuild).getBuildId(); will(returnValue(42L));
      allowing(myBuild).getSharedConfigParameters(); will(returnValue(Collections.unmodifiableMap(configParameters)));
      allowing(myBuild).addSharedConfigParameter(with(any(String.class)), with(any(String.class)));
      will(new CustomAction("Add config parameter") {
        public Object invoke(Invocation invocation) throws Throwable {
          configParameters.put((String)invocation.getParameter(0), (String)invocation.getParameter(1));
          return null;
        }
      });

      allowing(myNuGetTeamCityProvider).getNuGetRunnerPath(); will(returnValue(Paths.getNuGetRunnerPath()));
      allowing(psm).getGlobalPackageSources(myBuild); will(returnValue(Collections.unmodifiableCollection(myGlobalSources)));
    }});

    myCollector = new NuGetPackagesCollectorImpl();
    PackageUsages pu = new PackageUsagesImpl(
            myCollector,
            new NuGetPackagesConfigParser(),
            new PackageInfoLoader()
    );

    myExecutor = executingFactory();
    myActionFactory = new LoggingNuGetActionFactoryImpl(
            new NuGetActionFactoryImpl(
                    new NuGetAuthCommandBuildProcessFactory(
                            executingFactory(),
                            myNuGetTeamCityProvider,
                            psm,
                            new NuGetSourcesWriter()),
                    pu,
                    new CommandFactoryImpl()
            )
    );
  }

  protected void addGlobalSource(@NotNull final String feed) {
    addGlobalSource(feed, null, null);
  }

  protected void addGlobalSource(@NotNull final String feed,
                                 @Nullable final String user,
                                 @Nullable final String pass) {
    myGlobalSources.add(new PackageSource() {
      @NotNull
      public String getSource() {
        return feed;
      }

      @Nullable
      public String getUsername() {
        return user;
      }

      @Nullable
      public String getPassword() {
        return pass;
      }
    });
  }

  @NotNull
  protected File getTestDataPath(final String path) {
    return Paths.getTestDataPath("integration/" + path);
  }

  @NotNull
  private CommandlineBuildProcessFactory executingFactory() {
    return new CommandlineBuildProcessFactory() {
      @NotNull
      public BuildProcess executeCommandLine(@NotNull final BuildRunnerContext hostContext,
                                             @NotNull final String program,
                                             @NotNull final Collection<String> argz,
                                             @NotNull final File workingDir,
                                             @NotNull final Map<String, String> additionalEnvironment) throws RunBuildException {
        return new BuildProcessBase() {
          @NotNull
          @Override
          protected BuildFinishedStatus waitForImpl() throws RunBuildException {
            GeneralCommandLine cmd = new GeneralCommandLine();
            cmd.setExePath("cmd");
            cmd.addParameter("/c");
            cmd.addParameter(program);
            for (String arg : argz) {
              cmd.addParameter(arg.replaceAll("%+", "%"));
            }
            cmd.setWorkingDirectory(workingDir);

            Map<String, String> env = new HashMap<String, String>();
            env.putAll(System.getenv());
            env.putAll(additionalEnvironment);
            cmd.setEnvParams(env);

            System.out.println("Run: " + cmd.getCommandLineString());

            ExecResult result = SimpleCommandLineProcessRunner.runCommand(cmd, new byte[0]);

            System.out.println(result.getStdout());
            System.out.println(result.getStderr());

            myCommandsOutput.append(result.getStdout()).append("\n\n").append(result.getStderr()).append("\n\n");

            return result.getExitCode() == 0
                    ? BuildFinishedStatus.FINISHED_SUCCESS
                    : BuildFinishedStatus.FINISHED_FAILED;
          }
        };
      }
    };
  }
}
