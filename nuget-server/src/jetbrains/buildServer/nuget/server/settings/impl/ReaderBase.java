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

package jetbrains.buildServer.nuget.server.settings.impl;

import jetbrains.buildServer.nuget.server.settings.NuGetSettingsReader;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 30.10.11 14:42
 */
public abstract class ReaderBase implements NuGetSettingsReader {
  public boolean getBooleanParameter(@NotNull String key, boolean def) {
    try {
      final String s = getStringParameter(key);
      if (!StringUtil.isEmptyOrSpaces(s))
        return Boolean.valueOf(s);
    } catch (Exception e) {
      //NOP
    }
    return def;
  }

  public int getIntParameter(@NotNull String key, int def) {
    try {
      final String s = getStringParameter(key);
      if (!StringUtil.isEmptyOrSpaces(s))
        return Integer.parseInt(s);
    } catch (Exception e) {
      //NOP
    }
    return def;
  }
}
