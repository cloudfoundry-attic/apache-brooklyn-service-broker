package org.cloudfoundry.community.servicebroker.brooklyn.model;

import java.util.Map;

import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.Plan;

public abstract class BlueprintPlan extends Plan{
	
	public BlueprintPlan(String id, String name, String description,
			Map<String, Object> metadata) {
		super(id, name, description, metadata, true);
	}

	public abstract String toBlueprint(String brooklynCatalogId, String location, CreateServiceInstanceRequest request);

}
