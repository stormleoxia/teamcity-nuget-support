﻿using NUnit.Framework;

namespace JetBrains.TeamCity.NuGet.Tests
{
  [TestFixture]
  public class NuGetRunnerTest_1_4 : NuGetRunnerTestBase
  {
    protected override string NuGetExe
    {
      get { return Files.NuGetExe_1_4; }
    }
  }

  [TestFixture]
  public class NuGetRunnerTest_1_5 : NuGetRunnerTestBase
  {
    protected override string NuGetExe
    {
      get { return Files.NuGetExe_1_5; }
    }
  }
}
