package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import java.util.List;

import org.springframework.cloud.servicebroker.model.Plan;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;

public interface CatalogPlanStrategy {
	
	List<ServiceDefinition> makeServiceDefinitions();
    
    List<Plan> makePlans(String serviceId, String appName, Object yaml);

}
