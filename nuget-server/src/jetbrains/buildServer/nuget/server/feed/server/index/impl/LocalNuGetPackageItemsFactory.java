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

package jetbrains.buildServer.nuget.server.feed.server.index.impl;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.nuget.common.nuspec.Dependencies;
import jetbrains.buildServer.nuget.common.nuspec.Dependency;
import jetbrains.buildServer.nuget.common.nuspec.DependencyGroup;
import jetbrains.buildServer.nuget.common.nuspec.NuspecFileContent;
import jetbrains.buildServer.nuget.server.util.VersionUtility;
import jetbrains.buildServer.serverSide.SBuild;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static jetbrains.buildServer.nuget.server.feed.server.PackageAttributes.*;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com), re-written by evgeniy.koshkin
 * Date: 06.09.11 22:03
 */
public class LocalNuGetPackageItemsFactory implements NuGetPackageStructureAnalyser {
  private final Date myFinishDate;
  private final Map<String, String> myItems = new LinkedHashMap<String, String>();

  public LocalNuGetPackageItemsFactory(@NotNull Date finishDate) {
    myFinishDate = finishDate;
  }

  @NotNull
  public static LocalNuGetPackageItemsFactory createForBuild(@NotNull SBuild build){
    Date finishDate = build.getFinishDate();
    if(finishDate == null) finishDate = new Date();
    return new LocalNuGetPackageItemsFactory(finishDate);
  }

  @NotNull
  public Map<String, String> getItems() {
    return myItems;
  }

  public void analyseEntry(@NotNull String entryName) {
  }

  public void analyseNuspecFile(@NotNull NuspecFileContent nuspec) {
    //The list is generated from
    //JetBrains.TeamCity.NuGet.Feed.Tests.DumpRequiredPackageParameters2()
    //not included here: addItem(map, "TeamCityDownloadUrl", "");
    addItem(ID, nuspec.getId());
    final String version = nuspec.getVersion();
    addItem(VERSION, version);
    addItem(NORMALIZED_VERSION, VersionUtility.normalizeVersion(version));
    addItem(TITLE, nuspec.getTitle());
    addItem(RELEASE_NOTES, nuspec.getReleaseNotes());
    addItem(AUTHORS, nuspec.getAuthors());
    addItem(DEPENDENCIES, parseDependencies(nuspec));
    addItem(DESCRIPTION, nuspec.getDescription());
    addItem(COPYRIGHT, nuspec.getCopyright());
    addItem(PROJECT_URL, nuspec.getProjectUrl());
    addItem(TAGS, nuspec.getTags());
    addItem(ICON_URL, nuspec.getIconUrl());
    addItem(LICENSE_URL, nuspec.getLicenseUrl());
    addItem(REQUIRE_LICENSE_ACCEPTANCE, nuspec.getRequireLicenseAcceptance());
    //addItem("IsLatestVersion", "");
    addItem(LAST_UPDATED, ODataDataFormat.formatDate(myFinishDate));
    //addItem("Updated", formatDate(updated));
    addItem(MIN_CLIENT_VERSION, nuspec.getMinClientVersion());
  }

  private void addItem(@NotNull final String key, @Nullable final String value) {
    if (!StringUtil.isEmptyOrSpaces(value)) {
      myItems.put(key, value);
    }
  }

  @NotNull
  private String parseDependencies(@NotNull final NuspecFileContent nuspec) {
    final Dependencies dependencies = nuspec.getDependencies();
    if (dependencies == null) return "";
    final StringBuilder sb = new StringBuilder();
    processDependencies(dependencies.getDependencies(), null, sb);
    for (DependencyGroup group : dependencies.getGroups()) {
      processDependencies(group.getDependencies(), group.getTargetFramework(), sb);
    }
    return sb.toString();
  }

  private void processDependencies(@NotNull final Collection<Dependency> dependencies,
                                   @Nullable final String platform,
                                   @NotNull final StringBuilder sb) {
    for (Dependency dependency : dependencies) {
      final String id = dependency.getId();
      final String versionConstraint = dependency.getVersion();
      if (sb.length() != 0) sb.append("|");
      sb.append(id).append(":").append(versionConstraint);
      if (!StringUtil.isEmptyOrSpaces(platform)) {
        sb.append(":").append(platform);
      }
    }
  }
}
