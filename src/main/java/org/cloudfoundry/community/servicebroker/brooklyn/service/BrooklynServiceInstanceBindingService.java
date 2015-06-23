package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.Map;

import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceBindingRepository;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BrooklynServiceInstanceBindingService implements
		ServiceInstanceBindingService {
    
    private static final Logger LOG = LoggerFactory.getLogger(BrooklynServiceInstanceBindingService.class);
    
	private BrooklynRestAdmin admin;
	private BrooklynServiceInstanceBindingRepository repository;

	@Autowired
	public BrooklynServiceInstanceBindingService(BrooklynRestAdmin admin, BrooklynServiceInstanceBindingRepository repository) {
		this.admin = admin;
		this.repository = repository;

	}

	protected ServiceInstanceBinding getServiceInstanceBinding(String bindingId) {
		return repository.findOne(bindingId);
	}

	@Override
	public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest request)
			throws ServiceInstanceBindingExistsException, ServiceBrokerException {
		
		ServiceInstanceBinding serviceInstanceBinding = getServiceInstanceBinding(request.getBindingId());
		if (serviceInstanceBinding != null) {
			throw new ServiceInstanceBindingExistsException(serviceInstanceBinding);
		}
		
		LOG.info("creating service binding: [bindingId={}, serviceInstanceId={}, appGuid={}", 
		        request.getBindingId(), request.getServiceInstanceId(), request.getAppGuid()
        );
		
		Map<String, Object> credentials = admin.getApplicationSensors(request.getServiceDefinitionId());
		serviceInstanceBinding = new ServiceInstanceBinding(request.getBindingId(), request.getServiceInstanceId(), null, null, request.getAppGuid());
		repository.save(serviceInstanceBinding);
		return new ServiceInstanceBinding(request.getBindingId(), request.getServiceInstanceId(), credentials, null, request.getAppGuid());
	}

	@Override
	public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request)
			throws ServiceBrokerException {
		
		String bindingId = request.getBindingId();
        ServiceInstanceBinding serviceInstanceBinding = getServiceInstanceBinding(bindingId);
		if (serviceInstanceBinding != null) {
		    LOG.info("Deleting service binding: [BindingId={}]", bindingId);
			repository.delete(bindingId);
		}
		return serviceInstanceBinding;
	}

}
