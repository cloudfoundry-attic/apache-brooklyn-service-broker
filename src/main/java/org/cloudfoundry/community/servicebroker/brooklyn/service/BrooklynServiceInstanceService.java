package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.Map;
import java.util.concurrent.Future;

import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceRepository;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.Operations;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.OperationState;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceLastOperation;
import org.cloudfoundry.community.servicebroker.model.UpdateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import brooklyn.rest.domain.TaskSummary;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.exceptions.Exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.annotations.VisibleForTesting;

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
		LOG.info("creating service: [serviceInstanceId={}, planID={}, organizationGuid={}, spaceGuid={}", 
				request.getServiceInstanceId(),
				request.getPlanId(),
				request.getOrganizationGuid(),
				request.getSpaceGuid()
		);

        ServiceDefinition service = catalogService.getServiceDefinition(request.getServiceDefinitionId());

        String blueprint = createBlueprint(service, request);

        Future<TaskSummary> taskSummaryFuture = admin.createApplication(blueprint);
        TaskSummary taskSummary = ServiceUtil.getFutureValueLoggingError(taskSummaryFuture);
        // we set the service definition id to the entity id
        // as a handy way of associating the brooklyn entity
        // with this particular service instance.
        request.setServiceDefinitionId(taskSummary.getEntityId());
        ServiceInstanceLastOperation lastOp = new ServiceInstanceLastOperation(Operations.CREATING, OperationState.IN_PROGRESS);
        instance = new ServiceInstance(request)
                .withLastOperation(lastOp)
                .isAsync(true);
        repository.save(instance);
        return instance;

    }

    @VisibleForTesting
    public String createBlueprint(ServiceDefinition serviceDefinition, CreateServiceInstanceRequest request) {
        String location = "localhost"; // default
        Plan selectedPlan = null;
        for(Plan p : serviceDefinition.getPlans()){
            if(p.getId().equals(request.getPlanId())){
                selectedPlan = p;
            }
        }
        Map<String, Object> metadata = MutableMap.copyOf(selectedPlan.getMetadata());
        if (metadata.containsKey("location")) {
            location = metadata.remove("location").toString();
        }

        if (metadata.keySet().size() > 0) {
            ObjectWriter writer = new ObjectMapper().writer();
            String metadataJson = null;
            try {
                metadataJson = writer.writeValueAsString(metadata);
                return String.format("{\"services\":[\"type\": \"%s\"], \"locations\": [\"%s\"], \"brooklyn.config\":%s}", serviceDefinition.getId(), location, metadataJson);
            } catch (JsonProcessingException e) {
                throw Exceptions.propagate(e);
            }
        } else {
            return String.format("{\"services\":[\"type\": \"%s\"], \"locations\": [\"%s\"]}", serviceDefinition.getId(), location);
        }
    }

	@Override
	public ServiceInstance deleteServiceInstance(DeleteServiceInstanceRequest request) 
			throws ServiceBrokerException {
		
		String serviceInstanceId = request.getServiceInstanceId();
        ServiceInstanceLastOperation lastOp = new ServiceInstanceLastOperation(Operations.DELETING, OperationState.IN_PROGRESS);
        ServiceInstance instance = getServiceInstance(serviceInstanceId)
                .withLastOperation(lastOp)
                .isAsync(true);
        if (instance != null) {
            String entityId = instance.getServiceDefinitionId();
            admin.deleteApplication(entityId);
            LOG.info("Deleting service: [ServiceDefinitionId={}, ServiceInstanceId={}]", entityId, serviceInstanceId);
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
