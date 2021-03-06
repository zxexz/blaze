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
 package com.kodality.blaze.fhir.structure.defs;

import com.kodality.blaze.fhir.structure.api.ParseException;
import com.kodality.blaze.fhir.structure.api.ResourceRepresentation;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.r4.formats.XmlParser;
import org.hl7.fhir.r4.model.Resource;
import org.osgi.service.component.annotations.Component;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

@Component(immediate = true, service = { XmlRepresentation.class, ResourceRepresentation.class })
public class XmlRepresentation implements ResourceRepresentation {

  @Override
  public List<String> getMimeTypes() {
    return Arrays.asList("application/fhir+xml", "application/xml+fhir", "application/xml", "text/xml", "xml");
  }

  @Override
  public FhirFormat getFhirFormat() {
    return FhirFormat.XML;
  }

  @Override
  public String compose(Resource resource) {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      new XmlParser().compose(output, resource, true);
      return new String(output.toByteArray(), "UTF-8");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isParsable(String input) {
    String strip = StringUtils.stripStart(input, null);
    return StringUtils.startsWith(strip, "<");
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R extends Resource> R parse(String input) {
    try {
      return (R) new XmlParser().parse(input);
    } catch (Exception e) {
      throw new ParseException(e);
    }
  }

}
