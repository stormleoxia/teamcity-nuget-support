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

package jetbrains.buildServer.nuget.server.feed.server.javaFeed.functions;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.nuget.server.feed.server.NuGetServerSettings;
import jetbrains.buildServer.nuget.server.feed.server.index.NuGetIndexEntry;
import jetbrains.buildServer.nuget.server.feed.server.index.PackagesIndex;
import jetbrains.buildServer.nuget.server.feed.server.index.impl.SemanticVersionsComparators;
import jetbrains.buildServer.nuget.server.feed.server.javaFeed.MetadataConstants;
import jetbrains.buildServer.nuget.server.feed.server.javaFeed.PackageEntityEx;
import jetbrains.buildServer.nuget.server.feed.server.javaFeed.PackagesEntitySet;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.odata4j.core.OFunctionParameter;
import org.odata4j.core.OObject;
import org.odata4j.core.OSimpleObject;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.edm.EdmFunctionParameter;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;
import org.odata4j.producer.QueryInfo;

import java.util.*;

/**
 * @author Evgeniy.Koshkin
 */
public class GetUpdatesFunction implements NuGetFeedFunction {

  private static final String COLLECTION_VALUE_SEPARATOR = "|";
  private static final Comparator<String> SEMANTIC_VERSIONS_COMPARATOR = SemanticVersionsComparators.getSemanticVersionsComparator();

  @NotNull private final Logger LOG = Logger.getInstance(getClass().getName());
  @NotNull private final PackagesIndex myIndex;
  @NotNull private final NuGetServerSettings myServerSettings;

  public GetUpdatesFunction(@NotNull PackagesIndex index, @NotNull NuGetServerSettings serverSettings) {
    myIndex = index;
    myServerSettings = serverSettings;
  }

  @NotNull
  public String getName() {
    return MetadataConstants.GET_UPDATES_FUNCTION_NAME;
  }

  @NotNull
  public EdmFunctionImport.Builder generateImport(@NotNull EdmType returnType) {
    return new EdmFunctionImport.Builder()
            .setName(MetadataConstants.GET_UPDATES_FUNCTION_NAME)
            .setEntitySet(PackagesEntitySet.getBuilder())
            .setHttpMethod(MetadataConstants.HTTP_METHOD_GET)
            .setReturnType(returnType)
            .addParameters(new EdmFunctionParameter.Builder().setName(MetadataConstants.PACKAGE_IDS).setType(EdmSimpleType.STRING),
                    new EdmFunctionParameter.Builder().setName(MetadataConstants.VERSIONS).setType(EdmSimpleType.STRING),
                    new EdmFunctionParameter.Builder().setName(MetadataConstants.INCLUDE_PRERELEASE).setType(EdmSimpleType.BOOLEAN),
                    new EdmFunctionParameter.Builder().setName(MetadataConstants.INCLUDE_ALL_VERSIONS).setType(EdmSimpleType.BOOLEAN),
                    new EdmFunctionParameter.Builder().setName(MetadataConstants.TARGET_FRAMEWORKS).setType(EdmSimpleType.STRING),
                    new EdmFunctionParameter.Builder().setName(MetadataConstants.VERSION_CONSTRAINTS).setType(EdmSimpleType.STRING));
  }

  @Nullable
  public Iterable<Object> call(@NotNull EdmType returnType, @NotNull Map<String, OFunctionParameter> params, @Nullable QueryInfo queryInfo) {
    final List<String> packageIds = extractListOfStringsFromParamValue(params, MetadataConstants.PACKAGE_IDS);
    if(packageIds.isEmpty()) return null;
    final List<String> versions = extractListOfStringsFromParamValue(params, MetadataConstants.VERSIONS);
    if(versions.isEmpty()) return null;

    if(packageIds.size() != versions.size()){
      LOG.debug(String.format("Bad %s function call. Number of requested package IDs (%d) is not consistent with number of package versions (%d).",
              getName(), packageIds.size(), versions.size()));
      return null;
    }

    final boolean includeAllVersions = extractBooleanParameterValue(params, MetadataConstants.INCLUDE_ALL_VERSIONS);
    final List<NuGetIndexEntry> result = new ArrayList<NuGetIndexEntry>();

    for(int i = 0; i < packageIds.size(); i++){
      final String requestedVersion = versions.get(i);
      final String requestedPackageId = packageIds.get(i);
      final Iterator<NuGetIndexEntry> entryIterator = myIndex.getNuGetEntries(requestedPackageId);
      while (entryIterator.hasNext()){
        final NuGetIndexEntry indexEntry = entryIterator.next();
        if(SEMANTIC_VERSIONS_COMPARATOR.compare(requestedVersion, indexEntry.getPackageInfo().getVersion()) < 0){
          LOG.debug(String.format("Matched indexed package found fof id:%s version:%s. %s", requestedPackageId, requestedVersion, indexEntry));
          result.add(indexEntry);
          if(!includeAllVersions){
            LOG.debug(String.format("Not all the versions requested. Skip further index lookup for id:%s version:%s.", requestedPackageId, requestedVersion));
            break;
          }
        }
      }
    }

    if(result.isEmpty()){
      LOG.debug("No package updates found.");
      return null;
    }

    return CollectionsUtil.convertCollection(result, new Converter<Object, NuGetIndexEntry>() {
      public Object createFrom(@NotNull NuGetIndexEntry source) {
        return new PackageEntityEx(source, myServerSettings);
      }
    });
  }

  private boolean extractBooleanParameterValue(Map<String, OFunctionParameter> parameters, String parameterName) {
    return Boolean.valueOf(extractStringParameterValue(parameters, parameterName));
  }

  private List<String> extractListOfStringsFromParamValue(Map<String, OFunctionParameter> parameters, String parameterName){
    final String parameterValue = extractStringParameterValue(parameters, parameterName);
    if (parameterValue == null) return Collections.emptyList();
    else return StringUtil.split(parameterValue, COLLECTION_VALUE_SEPARATOR);
  }

  private String extractStringParameterValue(Map<String, OFunctionParameter> parameters, String parameterName){
    final OFunctionParameter parameter = parameters.get(parameterName);
    if(parameter == null){
      LOG.debug(String.format("Bad %s function call. %s parameter is not specified.", getName(), parameterName));
      return null;
    }
    final OObject valueObject = parameter.getValue();
    if(!(valueObject instanceof OSimpleObject))
    {
      LOG.debug(String.format("Bad %s function call. %s parameter type is invalid.", getName(), parameterName));
      return null;
    }
    return ((OSimpleObject) valueObject).getValue().toString();
  }
}