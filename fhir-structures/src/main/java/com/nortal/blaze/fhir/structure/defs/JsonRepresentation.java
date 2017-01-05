package com.nortal.blaze.fhir.structure.defs;

import com.nortal.blaze.fhir.structure.api.ParseException;
import com.nortal.blaze.fhir.structure.api.ResourceRepresentation;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.hl7.fhir.dstu3.formats.JsonParser;
import org.hl7.fhir.dstu3.model.Resource;

@Component(immediate = true)
@Service(ResourceRepresentation.class)
public class JsonRepresentation implements ResourceRepresentation {

  @Override
  public List<String> getMimeTypes() {
    return Arrays.asList("application/json+fhir", "application/json", "text/json", "json");
  }

  @Override
  public String compose(Resource resource) {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      new JsonParser().compose(output, resource);
      return new String(output.toByteArray(), "UTF-8");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isParsable(String input) {
    String strip = StringUtils.stripStart(input, null);
    return StringUtils.startsWithAny(strip, "{", "[");
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R extends Resource> R parse(String input) {
    try {
      return (R) new JsonParser().parse(input);
    } catch (Exception e) {
      throw new ParseException(e);
    }
  }

}