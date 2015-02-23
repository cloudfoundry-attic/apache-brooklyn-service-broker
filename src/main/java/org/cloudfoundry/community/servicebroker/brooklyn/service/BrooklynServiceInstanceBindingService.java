package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.Map;

import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceBindingRepository;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BrooklynServiceInstanceBindingService implements
		ServiceInstanceBindingService {


	private BrooklynRestAdmin admin;
	private BrooklynServiceInstanceBindingRepository repository;

	@Autowired
	public BrooklynServiceInstanceBindingService(BrooklynRestAdmin admin, BrooklynServiceInstanceBindingRepository repository) {
		this.admin = admin;
		this.repository = repository;

	}

	@Override
	public ServiceInstanceBinding createServiceInstanceBinding(String bindingId, ServiceInstance serviceInstance,
			String serviceId, String planId, String appGuid) throws ServiceInstanceBindingExistsException,
			ServiceBrokerException {

		ServiceInstanceBinding serviceInstanceBinding = getServiceInstanceBinding(bindingId);
		if (serviceInstanceBinding != null) {
			throw new ServiceInstanceBindingExistsException(
					serviceInstanceBinding);
		}
		Map<String, Object> credentials = admin.getApplicationSensors(serviceInstance.getServiceDefinitionId());
		serviceInstanceBinding = new ServiceInstanceBinding(bindingId, serviceInstance.getId(), null, null, appGuid);
		repository.save(serviceInstanceBinding);
		return new ServiceInstanceBinding(bindingId, serviceInstance.getId(), credentials, null, appGuid);
	}

	@Override
	public ServiceInstanceBinding deleteServiceInstanceBinding(
			String bindingId, ServiceInstance instance, String serviceId,
			String planId) throws ServiceBrokerException {
		
		ServiceInstanceBinding serviceInstanceBinding = getServiceInstanceBinding(bindingId);
		if (serviceInstanceBinding != null) {
			// do delete stuff
			repository.delete(bindingId);
		}
		return serviceInstanceBinding;
	}

	protected ServiceInstanceBinding getServiceInstanceBinding(String id) {
		return repository.findOne(id);
	}

}
