package org.cloudfoundry.community.servicebroker.brooklyn.model;

import java.util.Map;

import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserDefinedBlueprintPlan extends BlueprintPlan{
	
	private static final Logger LOG = LoggerFactory.getLogger(UserDefinedBlueprintPlan.class);

	public UserDefinedBlueprintPlan(String id, String name, String description,
			Map<String, Object> metadata) {
		super(id, name, description, metadata);
	}
	
	@Override
	public String toBlueprint(String brooklynCatalogId, String location, CreateServiceInstanceRequest request) {
		try {
			ObjectMapper om = new ObjectMapper();
			return om.writeValueAsString(request.getParameters());
		} catch (JsonProcessingException e) {
			LOG.error(e.getMessage());
			return "";
		}
	}

}
