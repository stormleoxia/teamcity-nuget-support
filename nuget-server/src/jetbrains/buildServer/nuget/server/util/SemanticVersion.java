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

package jetbrains.buildServer.nuget.server.util;

import com.google.common.base.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Evgeniy.Koshkin
 */
public class SemanticVersion implements Comparable<SemanticVersion> {
  private static Pattern VERSION_STRING_MATCHING_PATTERN = Pattern.compile("^([0-9]+)(?:\\.([0-9]+))?(?:\\.([0-9]+))?(?:\\.([0-9]+))?(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-\\.]+)?$", Pattern.CASE_INSENSITIVE);

  @NotNull private final Version myVersion;
  @Nullable private final String mySpecialVersion;
  @NotNull private final String myOriginalString;

  private SemanticVersion(@NotNull Version version, @Nullable String specialVersion, @NotNull String originalString) {
    myVersion = version;
    mySpecialVersion = specialVersion;
    myOriginalString = originalString;
  }

  @Nullable
  public static SemanticVersion valueOf(@NotNull String versionString) {
    if (Strings.isNullOrEmpty(versionString)) return null;

    final Matcher match = VERSION_STRING_MATCHING_PATTERN.matcher(versionString.trim());
    if (!match.find()) return null;

    int major = Integer.valueOf(match.group(1));
    final String minorString = match.group(2);
    int minor = (minorString != null) ? Integer.valueOf(minorString) : 0;
    final String patchString = match.group(3);
    int patch = (patchString != null) ? Integer.valueOf(patchString) : 0;
    final String buildString = match.group(4);
    int build = (buildString != null) ? Integer.valueOf(buildString) : 0;

    final Version versionValue = new Version(major, minor, patch, build);

    String release = match.group(5);
    if (release != null && release.startsWith("-"))
      release = release.substring(1);

    return new SemanticVersion(versionValue, release, versionString.replace(" ", ""));
  }

  public static int compareAsVersions(@NotNull String versionString1, @NotNull String versionString2) {
    final SemanticVersion version1 = valueOf(versionString1);
    final SemanticVersion version2 = valueOf(versionString2);
    return version1 != null && version2 != null ? version1.compareTo(version2) : versionString1.compareTo(versionString2);
  }

  @NotNull
  public Version getVersion() {
    return myVersion;
  }

  @Nullable
  public String getSpecialVersion() {
    return mySpecialVersion;
  }

  public int compareTo(@NotNull SemanticVersion other) {
    int result = myVersion.compareTo(other.myVersion);
    if (result != 0) return result;
    boolean empty = Strings.isNullOrEmpty(mySpecialVersion);
    boolean otherEmpty = Strings.isNullOrEmpty(other.mySpecialVersion);
    if (empty && otherEmpty) return 0;
    else if (empty) return 1;
    else if (otherEmpty) return -1;

    String[] o1 = split(mySpecialVersion);
    String[] o2 = split(other.mySpecialVersion);

    int x;
    for(int i = 0, max = Math.min(o1.length, o2.length); i < max; i++) {
      if ((x = compareElements(o1[i], o2[i]))!= 0) return x;
    }
    if (o1.length == 0 && o2.length > 0) return 1;
    if (o2.length == 0 && o1.length > 0) return -1;

    if (o1.length < o2.length) return -1;
    if (o1.length > o2.length) return 1;

    return 0;
  }

  public boolean equals(SemanticVersion that){
    if (mySpecialVersion != null ? !mySpecialVersion.equals(that.mySpecialVersion) : that.mySpecialVersion != null) return false;
    return myVersion.equals(that.myVersion);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return equals((SemanticVersion) o);
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 43 * hash + this.myVersion.hashCode();
    hash = 43 * hash + (this.mySpecialVersion != null ? this.mySpecialVersion.hashCode() : 0);
    return hash;
  }

  @Override
  public String toString()
  {
    return myOriginalString;
  }

  private int compareElements(@NotNull String s1, @NotNull String s2) {
    int i1 = 0;
    int i2 = 0;
    boolean isInt1 = true;
    boolean isInt2 = true;
    try {
      i1 = Integer.parseInt(s1);
    } catch (Exception e) {
      isInt1 = false;
    }
    try {
      i2 = Integer.parseInt(s2);
    } catch (Exception e) {
      isInt2 = false;
    }

    if (isInt1 && isInt2) {
      if (i1 == i2) return 0;
      if (i1 < i2) return -1;
      if (i1 > i2) return 1;
    }

    if (isInt1 && !isInt2) {
      return -1;
    }

    if (!isInt1 && isInt2) {
      return 1;
    }

    return s1.compareTo(s2);
  }

  @NotNull
  private String[] split(@Nullable String s) {
    if (s == null || s.length() == 0) return new String[0];
    return Pattern.compile("\\.").split(s);
  }
}
