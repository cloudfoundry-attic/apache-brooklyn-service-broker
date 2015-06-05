package org.cloudfoundry.community.servicebroker.brooklyn.repository;

import java.util.Map;

import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import brooklyn.rest.client.BrooklynApi;

@Service
public class BrooklynServiceInstanceRepository {
	
	private static final Logger LOG = LoggerFactory.getLogger(BrooklynServiceInstanceRepository.class);

	@Autowired
	private BrooklynApi restApi;
	private String application = "service-broker-records";
	private String entity = "service-instance-repository";
	
	
	@SuppressWarnings("unchecked")
	public ServiceInstance findOne(String serviceInstanceId) {
		Object object = restApi.getEntityConfigApi().get(application, entity, serviceInstanceId, false);
		if(object == null || !(object instanceof Map)) {
			LOG.info("Unable to get instance with serviceInstanceId={}", serviceInstanceId);
			return null;
		}
		
		Map<String, String> map = (Map<String, String>) object;	
		
		CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(
				map.get("serviceDefinitionId"), 
				map.get("planId"), 
				map.get("organizationGuid"), 
				map.get("spaceGuid"),
				true);
		return new ServiceInstance(request.withServiceInstanceId(serviceInstanceId));
		
	}
	
	public void delete(String serviceInstanceId) {
		try{
			restApi.getEntityConfigApi().set(application, entity, serviceInstanceId, false, "");
		} catch(Exception e){
			LOG.error("unable to delete {} {}", serviceInstanceId, e);
		}
	}

	
	public <S extends ServiceInstance> S save(S instance) {
		restApi.getEntityConfigApi().set(application, entity, instance.getServiceInstanceId(), false, instance);
		return instance;
	}
}
