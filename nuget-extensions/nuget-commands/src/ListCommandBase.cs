using System;
using System.Collections.Generic;
using System.ComponentModel.Composition;
using System.Linq;
using System.Linq.Expressions;
using JetBrains.TeamCity.NuGet.ExtendedCommands.Util;
using NuGet;

namespace JetBrains.TeamCity.NuGet.ExtendedCommands
{
  public abstract class ListCommandBase : CommandBase
  {
    [Import]
    public IPackageRepositoryFactory RepositoryFactory { get; set; }

    [Import]
    public IPackageSourceProvider SourceProvider { get; set; }

    protected IEnumerable<IPackage> GetAllPackages(string source, IEnumerable<string> ids)
    {
      IPackageRepository packageRepository = RepositoryFactory.CreateRepository(source);

      var param = Expression.Parameter(typeof (IPackageMetadata));

      Expression result = Expression.Constant(true);
      foreach (var id in ids)
      {
        result = Expression.Equal(Expression.Property(param, "Id"), Expression.Constant(id));
      }

      return packageRepository.GetPackages().Where(Expression.Lambda<Func<IPackage, bool>>(result, param));
    }

    protected static void PrintPackageInfo(IPackage p)
    {
      var msg = ServiceMessageFormatter.FormatMessage(
        "nuget-package",
        new ServiceMessageProperty("Id", p.Id),
        new ServiceMessageProperty("Version", p.Version.ToString())
        );

      System.Console.Out.WriteLine(msg);
    }

  }
}