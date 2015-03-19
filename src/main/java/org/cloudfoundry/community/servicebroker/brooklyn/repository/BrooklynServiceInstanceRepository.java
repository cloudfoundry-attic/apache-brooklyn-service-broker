package org.cloudfoundry.community.servicebroker.brooklyn.repository;

import java.util.Map;

import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import brooklyn.rest.client.BrooklynApi;

@Service
public class BrooklynServiceInstanceRepository {

	@Autowired
	private BrooklynApi restApi;
	private String application = "service-broker-records";
	private String entity = "service-instance-repository";
	
	
	public ServiceInstance findOne(String id) {
		Object object = restApi.getEntityConfigApi().get(application, entity, id, false);
		if(object == null || !(object instanceof Map)) return null;
		
		Map<String, String> map = (Map<String, String>) object;			
		return new ServiceInstance(
					map.get("id"), 
					map.get("serviceDefinitionId"), 
					map.get("planId"), 
					map.get("organizationGuid"), 
					map.get("spaceGuid"), 
					map.get("dashboardUrl"));
		
	}

	
	public void delete(String id) {
		try{
			restApi.getEntityConfigApi().set(application, entity, id, false, "");
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	
	public <S extends ServiceInstance> S save(S instance) {
		restApi.getEntityConfigApi().set(application, entity, instance.getId(), false, instance);
		return instance;
	}
}
