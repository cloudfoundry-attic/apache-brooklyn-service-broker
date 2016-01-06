package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import java.util.List;

import org.cloudfoundry.community.servicebroker.brooklyn.model.UserDefinedBlueprintPlan;
import org.springframework.cloud.servicebroker.model.Plan;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class UserDefinedPlanStrategy implements CatalogPlanStrategy{

	@Override
	public List<Plan> makePlans(String serviceId, String appName, Object yaml) {
		return ImmutableList.of(
				new UserDefinedBlueprintPlan("br_UserDefined", "UserDefined", "Allows users to specify a complete blueprint", ImmutableMap.of())
		);
	}

	@Override
	public List<ServiceDefinition> makeServiceDefinitions() {
		return ImmutableList.of(new ServiceDefinition("UserDefined", 
				"UserDefined", 
				"Allows users to specify a complete blueprint",
				true, 
				false, 
				makePlans("serviceId", null, "yaml"),
				ImmutableList.of(), 
				ImmutableMap.of(), 
				ImmutableList.of(), 
				null));
	}

}
