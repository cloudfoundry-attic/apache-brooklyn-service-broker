package org.cloudfoundry.community.servicebroker.brooklyn.service;

import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceRepository;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import brooklyn.rest.domain.TaskSummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BrooklynServiceInstanceService implements ServiceInstanceService {

	private static final Logger LOG = LoggerFactory.getLogger(BrooklynServiceInstanceService.class);

	private BrooklynRestAdmin admin;
	private BrooklynServiceInstanceRepository repository;
	private BrooklynCatalogService catalogService;
	
	@Autowired
	public BrooklynServiceInstanceService(BrooklynRestAdmin admin, BrooklynServiceInstanceRepository repository, BrooklynCatalogService catalogService) {
		this.admin = admin;
		this.repository = repository;
        this.catalogService = catalogService;
	}

	@Override
	public ServiceInstance getServiceInstance(String serviceInstanceId) {
		return repository.findOne(serviceInstanceId);
	}
	
	@Override
	public ServiceInstance createServiceInstance(CreateServiceInstanceRequest request)
			throws ServiceInstanceExistsException, ServiceBrokerException {
		
		admin.createRepositoryIfNotExists();

		// check if exists already
		ServiceInstance instance = getServiceInstance(request.getServiceInstanceId());
		if (instance != null) {
			throw new ServiceInstanceExistsException(instance);
		}
		Object parameters = request.getParameters(Object.class);
        LOG.info("creating service: [serviceInstanceId={}, planID={}, organizationGuid={}, spaceGuid={}, parameters={}", 
				request.getServiceInstanceId(),
				request.getPlanId(),
				request.getOrganizationGuid(),
				request.getSpaceGuid(),
				parameters
		);

		String location = "localhost"; // default
		ServiceDefinition service = catalogService.getServiceDefinition(request.getServiceDefinitionId());
		boolean userDefined = service.getId().equals("user-defined-service");
		
		String blueprint = "";
		if(userDefined){
		    // User defined services expect a blueprint in the parameters as JSON.
            ObjectMapper om = new ObjectMapper();
            try {
                blueprint = om.writeValueAsString(parameters);
            } catch (JsonProcessingException e) {
                throw new ServiceBrokerException(e);
            }
		} else {
		
            for (Plan p : service.getPlans()) {
                if (p.getId().equals(request.getPlanId())) {
                    location = p.getName();
                }
            }

            blueprint = String.format("{\"services\":[{\"type\": \"%s\"}], \"locations\": [\"%s\"]}", service.getId(), location);
        }
		TaskSummary taskSummary = admin.createApplication(blueprint);
		
		// we set the service definition id to the entity id 
		// as a handy way of associating the brooklyn entity
		// with this particular service instance.
		request.setServiceDefinitionId(taskSummary.getEntityId());
		instance = new ServiceInstance(request);
		repository.save(instance);
		return instance;
	}

	@Override
	public ServiceInstance deleteServiceInstance(DeleteServiceInstanceRequest request) 
			throws ServiceBrokerException {
		
		String serviceInstanceId = request.getServiceInstanceId();
        ServiceInstance instance = getServiceInstance(serviceInstanceId);
		if (instance != null) {
			repository.delete(serviceInstanceId);
			String entityId = instance.getServiceDefinitionId();
			LOG.info("Deleting service: [ServiceDefinitionId={}, ServiceInstanceId={}]", entityId, serviceInstanceId);
			admin.deleteApplication(entityId);
		}
		return instance;
	}

	@Override
	public ServiceInstance updateServiceInstance(UpdateServiceInstanceRequest request)
			throws ServiceInstanceUpdateNotSupportedException,
			ServiceBrokerException, ServiceInstanceDoesNotExistException {

		throw new ServiceInstanceUpdateNotSupportedException("Update not supported at this time");
	}

}
