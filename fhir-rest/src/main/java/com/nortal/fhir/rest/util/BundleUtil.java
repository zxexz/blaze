package com.nortal.fhir.rest.util;

import com.nortal.blaze.core.model.ResourceVersion;
import com.nortal.blaze.fhir.structure.api.ResourceComposer;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.Bundle.HTTPVerb;

import java.util.List;

public class BundleUtil {

  public static Bundle compose(Integer total, List<ResourceVersion> versions, BundleType bundleType) {
    Bundle bundle = new Bundle();
    bundle.setTotal(total == null ? versions.size() : total);
    bundle.setType(bundleType);

    for (ResourceVersion version : versions) {
      BundleEntryComponent entry = bundle.addEntry();
      entry.setResource(ResourceComposer.parse(version.getContent().getValue()));

      BundleEntryRequestComponent request = new BundleEntryRequestComponent();
      request.setMethod(version.isDeleted()
                                            ? HTTPVerb.DELETE
                                            : version.getId().getVersion() == 1 ? HTTPVerb.POST : HTTPVerb.PUT);
      //XXX: this is NOT how it should be.
      entry.setRequest(request);
    }
    return bundle;
  }
}
