package org.cloudfoundry.community.servicebroker.brooklyn.repository;

import java.util.Map;

import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import brooklyn.rest.client.BrooklynApi;

@Service
public class BrooklynServiceInstanceBindingRepository {

	@Autowired
	private BrooklynApi restApi;

	private String application = "service-broker-records";
	private String entity = "service-instance-binding-repository";
	
	@SuppressWarnings("unchecked")
	public ServiceInstanceBinding findOne(String bindingId) {
		Object object = restApi.getEntityConfigApi().get(application, entity, bindingId, false);
		if(object == null || !(object instanceof Map)) return null;
		
		Map<String, Object> map = (Map<String, Object>) object;
		return new ServiceInstanceBinding(
				(String)map.get("id"),
				(String)map.get("serviceInstanceId"),
				(Map<String, Object>)map.get("credentials"), 
			    (String)map.get("syslogDrainUrl"), 
				(String)map.get("appGuid"));
	}

	public <S extends ServiceInstanceBinding> S save(S serviceInstanceBinding) {	
		restApi.getEntityConfigApi().set(application, entity,
				serviceInstanceBinding.getServiceInstanceId(), false, serviceInstanceBinding);
		
		return serviceInstanceBinding;
	}

	public void delete(String bindingId) {
		try {
			restApi.getEntityConfigApi().set(application, entity, bindingId, false, "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



}
