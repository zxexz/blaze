/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package com.kodality.blaze.core.service.resource;

import com.kodality.blaze.core.exception.FhirException;
import com.kodality.blaze.core.model.search.QueryParam;
import com.kodality.blaze.core.service.conformance.CapabilitySearchConformance;
import com.kodality.blaze.core.service.conformance.ConformanceHolder;
import com.kodality.blaze.core.model.search.SearchCriterion;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.SearchParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public final class SearchUtil {
  private static final String MODIFIER = ":";
  private static final String CHAIN = ".";

  private SearchUtil() {
    //
  }

  /**
   * @return [resourceType, searchParam, (targetResourceType)]
   */
  public static String[] parseInclude(String include) {
    if (include == null) {
      return null;//fdsfds
    }
    String[] tokens = StringUtils.split(include, ":");
    if (tokens.length < 2 || tokens.length > 3) {
      String details = "_include parameter invalid. ResourceType:SearchParameter[:targetResourceType]";
      throw new FhirException(400, IssueType.PROCESSING, details);
    }
    return new String[] { tokens[0], tokens[1], tokens.length > 2 ? tokens[2] : null };
  }

  public static List<QueryParam> parse(String query, String resourceType) {
    Map<String, List<String>> params = new HashMap<>();
    Stream.of(query.split("&")).forEach(q -> {
      String[] qr = q.split("=");
      if (qr.length != 2) {
        throw new FhirException(400, IssueType.PROCESSING, "invalid query parameter: '" + q + "' in '?" + query + "'");
      }
      params.computeIfAbsent(qr[0], (a) -> new ArrayList<>()).add(qr[1]);
    });
    return parse(params, resourceType);
  }

  public static List<QueryParam> parse(Map<String, List<String>> params, String resourceType) {
    if (params == null || params.isEmpty()) {
      return Collections.emptyList();
    }
    params.remove("");
    params.remove(null);// well this is strange

    List<QueryParam> result = new ArrayList<>();
    params.forEach((k, v) -> result.addAll(parse(k, v, resourceType)));
    return result;
  }

  public static List<QueryParam> parse(String rawKey, List<String> rawValues, String resourceType) {
    ChainForge chainsmith = buildForge(rawKey, resourceType);
    return rawValues.stream().map(value -> {
      QueryParam param = chainsmith.forge();
      param.setValues(Arrays.asList(StringUtils.split(value, ",")));
      return param;
    }).collect(toList());
  }

  private static ChainForge buildForge(String chain, String resourceType) {
    String link = StringUtils.substringBefore(chain, CHAIN);
    String key = StringUtils.substringBefore(link, MODIFIER);
    String modifier = link.contains(MODIFIER) ? StringUtils.substringAfter(link, MODIFIER) : null;

    if (SearchCriterion.resultParamKeys.contains(key)) {
      return new ChainForge(key, modifier, null, resourceType);
    }

    CapabilityStatementRestResourceSearchParamComponent conformance =
        CapabilitySearchConformance.get(resourceType, key);
    SearchParameter sp = ConformanceHolder.requireSearchParam(resourceType, key);
    validate(conformance, sp, key, modifier);

    ChainForge forge = new ChainForge(key, modifier, conformance.getType(), resourceType);
    if (chain.contains(CHAIN) && conformance.getType() == SearchParamType.REFERENCE) {
      String remainder = chain.contains(CHAIN) ? StringUtils.substringAfter(chain, CHAIN) : null;
      List<String> targetResourceTypes = sp.getTarget().stream().map(ct -> ct.getValue()).collect(toList());
      if (modifier != null) {
        targetResourceTypes.retainAll(Collections.singletonList(modifier));
      }
      if (targetResourceTypes.isEmpty()) {
        throw new FhirException(400, IssueType.PROCESSING, key + ":" + modifier + "not defined");
      }
      targetResourceTypes.forEach(rt -> forge.nextLink(buildForge(remainder, rt)));
    }
    return forge;
  }

  private static void validate(CapabilityStatementRestResourceSearchParamComponent conformance,
                               SearchParameter sp,
                               String key,
                               String modifier) {
    if (conformance == null) {
      String details = "search parameter '" + key + "' not supported by conformance";
      throw new FhirException(400, IssueType.NOTSUPPORTED, details);
    }
    if (!validateModifier(conformance, sp, modifier)) {
      String details = "modifier '" + modifier + "' not supported by conformance for '" + key + "'";
      throw new FhirException(400, IssueType.NOTSUPPORTED, details);
    }
  }

  private static boolean validateModifier(CapabilityStatementRestResourceSearchParamComponent conformance,
                                          SearchParameter sp,
                                          String modifier) {
    if (StringUtils.isEmpty(modifier)) {
      return true;
    }
    if (conformance.getType() == SearchParamType.REFERENCE) {
      return CollectionUtils.isEmpty(sp.getTarget())
          || sp.getTarget().stream().anyMatch(t -> t.getValue().equals(modifier));
    }
    // FIXME gone from searchparameters?
    return true;
    //    return sp.getModifier().stream().anyMatch(m -> m.getValue().toCode().equals(modifier));
  }

  private static class ChainForge {
    private final String key;
    private final String modifier;
    private final SearchParamType paramType;
    private final String resourceType;

    private List<ChainForge> next;

    public ChainForge(String key, String modifier, SearchParamType paramType, String resourceType) {
      this.key = key;
      this.modifier = modifier;
      this.paramType = paramType;
      this.resourceType = resourceType;
    }

    public void nextLink(ChainForge next) {
      if (this.next == null) {
        this.next = new ArrayList<>();
      }
      this.next.add(next);
    }

    public QueryParam forge() {
      QueryParam param = new QueryParam(key, modifier, paramType, resourceType);
      if (next != null) {
        next.forEach(n -> param.addChain(n.forge()));
      }
      return param;
    }

  }

}
