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
 package com.kodality.blaze.service.conformance;

import com.kodality.blaze.core.service.conformance.ConformanceHolder;
import org.hl7.fhir.r4.model.SearchParameter;

import java.util.HashMap;

public class TestConformanceHolder extends ConformanceHolder {

  public static void apply(SearchParameter sp) {
    sp.getBase().forEach(ct -> {
      searchParams.putIfAbsent(ct.getValue(), new HashMap<String, SearchParameter>());
      searchParams.get(ct.getValue()).put(sp.getCode(), sp);
    });
  }
}
