package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.Map;
import java.util.concurrent.Future;

import org.apache.brooklyn.rest.domain.TaskSummary;
import org.cloudfoundry.community.servicebroker.brooklyn.model.BlueprintPlan;
import org.cloudfoundry.community.servicebroker.brooklyn.model.BrooklynServiceInstance;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceRepository;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.Operations;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceExistsException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.OperationState;
import org.springframework.cloud.servicebroker.model.Plan;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

	public BrooklynServiceInstance getServiceInstance(String serviceInstanceId) {
		return repository.findOne(serviceInstanceId);
	}
	
	@Override
	public CreateServiceInstanceResponse createServiceInstance(CreateServiceInstanceRequest request) {
		admin.createRepositoryIfNotExists();
		// check if exists already
		BrooklynServiceInstance instance = getServiceInstance(request.getServiceInstanceId());
		if (instance != null) {
			throw new ServiceInstanceExistsException(instance.getServiceInstanceId(), instance.getServiceDefinitionId());
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
		instance = new BrooklynServiceInstance(request.getServiceInstanceId(), request.getServiceDefinitionId())
            .withEntityId(taskSummary.getEntityId());
        repository.save(instance.withOperation(Operations.CREATING).withOperationStatus(OperationState.IN_PROGRESS));
		return new CreateServiceInstanceResponse(true);
    }

	@Override
	public GetLastServiceOperationResponse getLastOperation(GetLastServiceOperationRequest request) {
        try {
            OperationState serviceInstanceLastOperation = getServiceInstance(request.getServiceInstanceId()).getOperationState();
            return new GetLastServiceOperationResponse(serviceInstanceLastOperation);
        } catch (Exception e) {
            LOG.info("exception thrown getting last operation for {}: {}", request, e);
            throw e;
        }
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

        Map<String, Object> metadata = serviceDefinition.getMetadata();
        String brooklynCatalogId = (String) metadata.get("brooklynCatalogId");
		String blueprint = ((BlueprintPlan)selectedPlan).toBlueprint(brooklynCatalogId, location, request);
        LOG.info("launching from blueprint: [blueprint={}]", blueprint);
		return blueprint;
    }

	@Override
	public DeleteServiceInstanceResponse deleteServiceInstance(DeleteServiceInstanceRequest request) {
		String serviceInstanceId = request.getServiceInstanceId();
		BrooklynServiceInstance instance = getServiceInstance(serviceInstanceId);
        if (instance != null) {
            instance = instance.withOperation(Operations.DELETING).withOperationStatus(OperationState.IN_PROGRESS);
            String entityId = instance.getEntityId();
            admin.deleteApplication(entityId);
            LOG.info("Deleting service: [Entity={}, ServiceInstanceId={}]", entityId, serviceInstanceId);
		}
        repository.save(instance);
		return new DeleteServiceInstanceResponse(true);
	}

	@Override
	public UpdateServiceInstanceResponse updateServiceInstance(UpdateServiceInstanceRequest request) {
		throw new ServiceInstanceUpdateNotSupportedException("Update not supported at this time");
	}

}
