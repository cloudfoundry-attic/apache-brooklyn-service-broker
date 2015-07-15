package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import java.util.List;

import org.cloudfoundry.community.servicebroker.model.Plan;

public interface CatalogPlanStrategy {
    
    List<Plan> makePlans(String serviceId, String yaml);

}
