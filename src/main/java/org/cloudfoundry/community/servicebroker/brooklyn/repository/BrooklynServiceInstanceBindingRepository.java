package org.cloudfoundry.community.servicebroker.brooklyn.repository;

import java.util.Map;

import org.apache.brooklyn.rest.client.BrooklynApi;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


// TODO Consider using @Async for access to REST api
@Service
public class BrooklynServiceInstanceBindingRepository {
    
    private static final Logger LOG = LoggerFactory.getLogger(BrooklynServiceInstanceBindingRepository.class);

	@Autowired
	private BrooklynApi restApi;

	private String application = "service-broker-records";
	private String entity = "service-instance-binding-repository";
	
	@SuppressWarnings("unchecked")
	public ServiceInstanceBinding findOne(String bindingId) {
		Object object;
		try{
			object = restApi.getEntityConfigApi().get(application, entity, bindingId, false);
		} catch(Exception e) {
		    LOG.error("Unable to get instance with bindingId={}", bindingId);
		    return null;
		}
		if(object == null || !(object instanceof Map)) {
		    LOG.error("Unable to get instance with bindingId={}", bindingId);
		    return null;
		}
		
		Map<String, Object> map = (Map<String, Object>) object;
		return new ServiceInstanceBinding(
				(String)map.get("id"),
				(String)map.get("serviceInstanceId"),
				(Map<String, Object>)map.get("credentials"), 
			    (String)map.get("syslogDrainUrl"), 
				(String)map.get("appGuid"));
	}

	public <S extends ServiceInstanceBinding> S save(S serviceInstanceBinding) {
		try{
			restApi.getEntityConfigApi().set(application, entity,
				serviceInstanceBinding.getServiceInstanceId(), false, serviceInstanceBinding);
			return serviceInstanceBinding;
		} catch(Exception e){
			LOG.error("unable to save {} {}", serviceInstanceBinding, e);
			return null;
		}
	}

	public void delete(String bindingId) {
		try {
			restApi.getEntityConfigApi().set(application, entity, bindingId, false, "");
		} catch (Exception e) {
			LOG.error("unable to delete {} {}", bindingId, e);
		}
	}



}
