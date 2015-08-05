package org.cloudfoundry.community.servicebroker.brooklyn.model;

import java.util.Map;

import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.Plan;

import brooklyn.util.collections.MutableMap;
import brooklyn.util.exceptions.Exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public abstract class BlueprintPlan extends Plan{
	
	public BlueprintPlan(String id, String name, String description,
			Map<String, Object> metadata) {
		super(id, name, description, metadata);
	}

	public abstract String toBlueprint(String location, CreateServiceInstanceRequest request);

}
