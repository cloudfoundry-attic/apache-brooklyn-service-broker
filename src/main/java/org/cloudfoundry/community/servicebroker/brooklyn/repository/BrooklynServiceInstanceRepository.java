package org.cloudfoundry.community.servicebroker.brooklyn.repository;

import java.util.Map;
import java.util.concurrent.Future;

import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.brooklyn.service.ServiceUtil;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.OperationState;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceLastOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSet;

@Service
public class BrooklynServiceInstanceRepository {
	
	private static final Logger LOG = LoggerFactory.getLogger(BrooklynServiceInstanceRepository.class);

	private BrooklynRestAdmin restAdmin;
	private String application = "service-broker-records";
	private String entity = "service-instance-repository";
	

	@Autowired
	public BrooklynServiceInstanceRepository(BrooklynRestAdmin restApi) {
		this.restAdmin = restApi;
	}

    public ServiceInstance findOne(String serviceInstanceId) {
        return  findOne(serviceInstanceId, true);
    }

	@SuppressWarnings("unchecked")
	public ServiceInstance findOne(String serviceInstanceId, boolean includeEverything) {
		Future<Map<String, Object>> serviceInstanceFuture = restAdmin.getConfigAsMap(application, entity, serviceInstanceId);
		Map<String, Object> map = ServiceUtil.getFutureValueLoggingError(serviceInstanceFuture);
		if(map == null)  return null;

        String brooklynEntity = String.valueOf(map.get("serviceDefinitionId"));
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(
                brooklynEntity,
				map.get("planId").toString(),
				map.get("organizationGuid").toString(),
				map.get("spaceGuid").toString(),
                true
				);
        if(!includeEverything) {
            return new ServiceInstance(request.withServiceInstanceId(serviceInstanceId));
        }

        Map<String, Object> previousOperationMap = (Map<String, Object>)map.get("serviceInstanceLastOperation");
        
        String currentState = previousOperationMap.get("description").toString();
        String serviceStatus = ServiceUtil.getFutureValueLoggingError(restAdmin.getServiceState(brooklynEntity));
        if(serviceStatus == null) {
        	serviceStatus = "DESTROYED";
        }
        OperationState state = OperationState.FAILED;
        boolean deleted = false;
        String dashboardUrl = null;
        state = nextState(serviceStatus, currentState);
        
        if(state.equals(OperationState.SUCCEEDED)){
        	if(serviceStatus.equals("DESTROYED")){
        		delete(serviceInstanceId);
        		deleted = true;
        	} else {
        		dashboardUrl = ServiceUtil.getFutureValueLoggingError(restAdmin.getDashboardUrl(brooklynEntity));
        	}
        }

        ServiceInstanceLastOperation lastOp = new ServiceInstanceLastOperation(serviceStatus, state);
        ServiceInstance newInstance = new ServiceInstance(request.withServiceInstanceId(serviceInstanceId))  
        	.withDashboardUrl(dashboardUrl)
            .withLastOperation(lastOp)
            .isAsync(true);

        if(!deleted)
        	save(newInstance);
        return newInstance;
	}
	
	public void delete(String serviceInstanceId) {
		restAdmin.deleteConfig(application, entity, serviceInstanceId);
	}

	
	public <S extends ServiceInstance> S save(S instance) {
		Object response = ServiceUtil.getFutureValueLoggingError(restAdmin.setConfig(application, entity, instance.getServiceInstanceId(), instance));
		return (S) response;
	}
	
	private OperationState nextState(String serviceStatus, String currentState) {
		currentState = currentState.toUpperCase();
		OperationState nextState = OperationState.FAILED;
		switch(currentState){
		case "CREATING":
		case "CREATED:":
		case "STARTING":
		case "RUNNING":
			if (ImmutableSet.of("CREATED", "STARTING").contains(serviceStatus)) {
				nextState = OperationState.IN_PROGRESS;
			}

			if (serviceStatus.equals("RUNNING")) {
				nextState = OperationState.SUCCEEDED;
			}
			break; // all other statuses transition to fail
		case "DELETING":
		case "STOPPING":
		case "STOPPED":
			if (ImmutableSet.of("STOPPING", "STOPPED").contains(serviceStatus)) {
				nextState = OperationState.IN_PROGRESS;
			}

			if (serviceStatus.equals("DESTROYED")) {
				nextState = OperationState.SUCCEEDED;
			}		
		}
		LOG.info("currentState={}, serviceStatus={}, nextState={}", currentState, serviceStatus, nextState);
		return nextState;
	}
}
