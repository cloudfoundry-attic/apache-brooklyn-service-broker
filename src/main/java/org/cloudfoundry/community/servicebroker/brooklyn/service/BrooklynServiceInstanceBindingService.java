package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceBindingRepository;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceRepository;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
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
	private BrooklynServiceInstanceBindingRepository bindingRepository;
    private BrooklynServiceInstanceRepository instanceRepository;

	@Autowired
	public BrooklynServiceInstanceBindingService(BrooklynRestAdmin admin, BrooklynServiceInstanceBindingRepository bindingRepository, BrooklynServiceInstanceRepository instanceRepository) {
		this.admin = admin;
		this.bindingRepository = bindingRepository;
        this.instanceRepository = instanceRepository;

	}

	protected ServiceInstanceBinding getServiceInstanceBinding(String bindingId) {
		return bindingRepository.findOne(bindingId);
	}

	@Override
	public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest request)
			throws ServiceInstanceBindingExistsException, ServiceBrokerException {
		
		ServiceInstanceBinding serviceInstanceBinding = getServiceInstanceBinding(request.getBindingId());
		if (serviceInstanceBinding != null) {
			throw new ServiceInstanceBindingExistsException(serviceInstanceBinding);
		}
		
		ServiceInstance serviceInstance = instanceRepository.findOne(request.getServiceInstanceId());
		String entityId = serviceInstance.getServiceDefinitionId();
		
		LOG.info("creating service binding: [entity={}, serviceDefinitionId={}, bindingId={}, serviceInstanceId={}, appGuid={}", 
		      entityId, request.getServiceDefinitionId(), request.getBindingId(), request.getServiceInstanceId(), request.getAppGuid()
        );
		
		Future<Map<String, Object>> credentialsFuture = admin.getApplicationSensors(entityId);
        Map<String, Object> credentials = ServiceUtil.getFutureValueLoggingError(credentialsFuture);
        serviceInstanceBinding = new ServiceInstanceBinding(request.getBindingId(), request.getServiceInstanceId(), null, null, request.getAppGuid());
		bindingRepository.save(serviceInstanceBinding);
		return new ServiceInstanceBinding(request.getBindingId(), request.getServiceInstanceId(), credentials, null, request.getAppGuid());
	}

	@Override
	public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request)
			throws ServiceBrokerException {
		
		String bindingId = request.getBindingId();
        ServiceInstanceBinding serviceInstanceBinding = getServiceInstanceBinding(bindingId);
		if (serviceInstanceBinding != null) {
		    LOG.info("Deleting service binding: [BindingId={}]", bindingId);
			bindingRepository.delete(bindingId);
		}
		return serviceInstanceBinding;
	}

}
