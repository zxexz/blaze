package com.nortal.fhir.conformance.definition;

import com.nortal.blaze.core.util.EtcMonitor;
import com.nortal.blaze.fhir.structure.service.ResourceRepresentationService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.validation.ProfileValidator;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Component(immediate = true)
public class ResourceDefinitionsMonitor extends EtcMonitor {
  private static final Map<String, StructureDefinition> definitions = new HashMap<>();
  @Reference
  private ResourceRepresentationService representationService;
  @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
  private final List<ResourceDefinitionListener> listeners = new ArrayList<>();

  public ResourceDefinitionsMonitor() {
    super("definitions");
  }

  @Activate
  private void init() {
    start();
  }

  @Deactivate
  private void destroy() {
    stop();
  }

  public static List<StructureDefinition> get() {
    return new ArrayList<StructureDefinition>(definitions.values());
  }

  public static StructureDefinition getDefinition(String type) {
    return definitions.get(type);
  }

  @Override
  protected void clear() {
    definitions.clear();
  }

  @Override
  protected void file(File file) {
    Resource res = representationService.parse(readFile(file));

    List<StructureDefinition> defs = new ArrayList<>();

    if (ResourceType.StructureDefinition == res.getResourceType()) {
      defs.add((StructureDefinition) res);
    }
    if (ResourceType.Bundle == res.getResourceType()) {
      ((Bundle) res).getEntry().stream().forEach(e -> {
        if (ResourceType.StructureDefinition == e.getResource().getResourceType()) {
          defs.add((StructureDefinition) e.getResource());
        }
      });
    }

    defs.forEach(def -> validate(def));
    definitions.putAll(defs.stream().filter(d -> !"group".equalsIgnoreCase(d.getName())).collect(toMap(def -> def.getName(),
                                                                                                       def -> def)));
  }

  @Override
  protected void finish() {
    listeners.forEach(l -> l.comply(get()));
  }

  private void validate(StructureDefinition definition) {
    List<ValidationMessage> errors = new ProfileValidator().validate(definition, false);
    if (CollectionUtils.isEmpty(errors)) {
      return;
    }
    throw new RuntimeException(errors.stream().map(e -> e.getMessage()).collect(Collectors.joining(",")));
  }

  protected void bind(ResourceDefinitionListener listener) {
    listeners.add(listener);
  }

  protected void unbind(ResourceDefinitionListener listener) {
    listeners.remove(listener);
  }

  private String readFile(File file) {
    try {
      return FileUtils.readFileToString(file, "UTF8");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
