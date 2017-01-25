package org.cloudfoundry.community.servicebroker.brooklyn.model;

import java.util.Map;

import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.style.ToStringCreator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class DefaultBlueprintPlan extends BlueprintPlan{
	
	private static final Logger LOG = LoggerFactory.getLogger(DefaultBlueprintPlan.class);
	private String appName;

	public DefaultBlueprintPlan(String id, String name, String description, String appName,
			Map<String, Object> metadata) {
		super(id, name, description, metadata);
		this.appName = appName;
	}

	@Override
	public String toBlueprint(String brooklynCatalogId, String location, 
			CreateServiceInstanceRequest request) {
		ObjectWriter writer = new ObjectMapper().writer();
		
		Map<String, Object> metadata = MutableMap.copyOf(getMetadata());
		String name = (appName + " (CF Service)").trim();
        try {
			if (metadata.containsKey("location")) {
				location = writer.writeValueAsString(metadata.remove("location"));
			} else {
				location = writer.writeValueAsString(location);
			}
			
		} catch (JsonProcessingException e) {
        	LOG.error("unable to make location: {}",  e.getMessage());
            Exceptions.propagateIfFatal(e);
		}
        
        Map<Object, Object> config = MutableMap.of();
        // add parameters
        // TODO sanitize this input from user
        
		Map<?, ?> parameters = request.getParameters();
		if(parameters != null){
			config.putAll(parameters);
		}
        config.putAll(metadata);
        String blueprint = "";
        if (config.keySet().size() > 0) {      
            String configJson = null;
            try {
                configJson = writer.writeValueAsString(config);
                blueprint = String.format("{\"name\":\"%s\", \"services\":[{\"type\": \"%s\"}], \"locations\": [%s], \"brooklyn.config\":%s}", name, brooklynCatalogId, location, configJson);
            } catch (JsonProcessingException e) {
            	LOG.error("unable to add config: {}",  e.getMessage());
                Exceptions.propagateIfFatal(e);
            }
        } else {
        	blueprint = String.format("{\"name\":\"%s\", \"services\":[{\"type\": \"%s\"}], \"locations\": [%s]}", name, brooklynCatalogId, location);
        }
        
        return blueprint;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("id", getId())
				.append("name", getName())
				.append("description", getDescription())
				.append("appName", appName)
				.append("metadata", getMetadata())
				.toString();
	}
}
