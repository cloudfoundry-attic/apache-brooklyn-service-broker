package org.cloudfoundry.community.servicebroker.brooklyn.model;

import java.util.Map;

import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class DefaultBlueprintPlan extends BlueprintPlan{

	public DefaultBlueprintPlan(String id, String name, String description,
			Map<String, Object> metadata) {
		super(id, name, description, metadata);
	}

	@Override
	public String toBlueprint(String brooklynCatalogId, String location,
			CreateServiceInstanceRequest request) {
		Map<String, Object> metadata = MutableMap.copyOf(getMetadata());
        if (metadata.containsKey("location")) {
            location = metadata.remove("location").toString();
        }
        
        Map<Object, Object> config = MutableMap.of();
        // add parameters
        // TODO sanitize this input from user
        
		Map<?, ?> parameters = request.getParameters();
		if(parameters != null){
			config.putAll(parameters);
		}
        config.putAll(metadata);
        
        if (config.keySet().size() > 0) {
            ObjectWriter writer = new ObjectMapper().writer();
            String configJson = null;
            try {
                configJson = writer.writeValueAsString(config);
                return String.format("{\"services\":[{\"type\": \"%s\"}], \"locations\": [\"%s\"], \"brooklyn.config\":%s}", brooklynCatalogId, location, configJson);
            } catch (JsonProcessingException e) {
                throw Exceptions.propagate(e);
            }
        } else {
            return String.format("{\"services\":[{\"type\": \"%s\"}], \"locations\": [\"%s\"]}", brooklynCatalogId, location);
        }
	}

}
