package org.cloudfoundry.community.servicebroker.brooklyn.repository;

import java.util.Map;
import java.util.concurrent.Future;

import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.brooklyn.service.ServiceUtil;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BrooklynServiceInstanceBindingRepository {
    
    private static final Logger LOG = LoggerFactory.getLogger(BrooklynServiceInstanceBindingRepository.class);

	private BrooklynRestAdmin restAdmin;
	private String application = "service-broker-records";
	private String entity = "service-instance-binding-repository";
	
	@Autowired
	public BrooklynServiceInstanceBindingRepository(BrooklynRestAdmin restApi) {
		this.restAdmin = restApi;
	}
	
	@SuppressWarnings("unchecked")
	public ServiceInstanceBinding findOne(String bindingId) {
		Future<Map<String, Object>> serviceBindingFuture = restAdmin.getConfigAsMap(application, entity, bindingId);
		Map<String, Object> map = ServiceUtil.getFutureValueLoggingError(serviceBindingFuture);
		if (map == null) return null;
		
		return new ServiceInstanceBinding(
				(String)map.get("id"),
				(String)map.get("serviceInstanceId"),
				(Map<String, Object>)map.get("credentials"), 
			    (String)map.get("syslogDrainUrl"), 
				(String)map.get("appGuid"));
	}

	public <S extends ServiceInstanceBinding> S save(S serviceInstanceBinding) {
		Object object = ServiceUtil.getFutureValueLoggingError(restAdmin.setConfig(application, entity, serviceInstanceBinding.getId(), serviceInstanceBinding));
		return (S)object;
	}

	public void delete(String bindingId) {
		restAdmin.deleteConfig(application, entity, bindingId);
	}



}
