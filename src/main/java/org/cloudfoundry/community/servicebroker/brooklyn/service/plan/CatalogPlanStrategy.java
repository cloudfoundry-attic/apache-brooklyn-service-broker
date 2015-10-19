package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import java.util.List;

import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;

public interface CatalogPlanStrategy {
	
	List<ServiceDefinition> makeServiceDefinitions();
    
    List<Plan> makePlans(String serviceId, Object yaml);

}
