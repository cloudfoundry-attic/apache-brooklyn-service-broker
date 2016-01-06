package org.cloudfoundry.community.servicebroker.brooklyn.repository;

import java.util.Map;
import java.util.concurrent.Future;

import org.cloudfoundry.community.servicebroker.brooklyn.model.BrooklynServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.brooklyn.service.ServiceUtil;
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
	public BrooklynServiceInstanceBinding findOne(String bindingId) {
		Future<Map<String, Object>> serviceBindingFuture = restAdmin.getConfigAsMap(application, entity, bindingId);
		Map<String, Object> map = ServiceUtil.getFutureValueLoggingError(serviceBindingFuture);
		if (map == null) return null;
		
		return new BrooklynServiceInstanceBinding(
				(String)map.get("id"),
				(String)map.get("serviceInstanceId"),
				(Map<String, Object>)map.get("credentials"),
				(String)map.get("appGuid"));
	}

	public BrooklynServiceInstanceBinding save(BrooklynServiceInstanceBinding serviceInstanceBinding) {
		return (BrooklynServiceInstanceBinding) ServiceUtil.getFutureValueLoggingError(restAdmin.setConfig(application, entity, serviceInstanceBinding.getServiceBindingId(), serviceInstanceBinding));

	}

	public void delete(String bindingId) {
		restAdmin.deleteConfig(application, entity, bindingId);
	}



}
