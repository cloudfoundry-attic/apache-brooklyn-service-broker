package org.cloudfoundry.community.servicebroker.brooklyn.model;

import java.util.Map;

import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.Plan;

public abstract class BlueprintPlan extends Plan{
	
	public BlueprintPlan(String id, String name, String description,
			Map<String, Object> metadata) {
		super(id, name, description, metadata);
	}

	public abstract String toBlueprint(String location, CreateServiceInstanceRequest request);

}
