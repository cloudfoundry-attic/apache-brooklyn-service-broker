package org.cloudfoundry.community.servicebroker.brooklyn.repository;

import java.util.Map;
import java.util.concurrent.Future;

import org.cloudfoundry.community.servicebroker.brooklyn.model.BrooklynServiceInstance;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.brooklyn.service.ServiceUtil;
import org.springframework.cloud.servicebroker.model.OperationState;
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

    public BrooklynServiceInstance findOne(String serviceInstanceId) {
        return  findOne(serviceInstanceId, true);
    }

	@SuppressWarnings("unchecked")
	public BrooklynServiceInstance findOne(String serviceInstanceId, boolean includeEverything) {
		Future<Map<String, Object>> serviceInstanceFuture = restAdmin.getConfigAsMap(application, entity, serviceInstanceId);
		Map<String, Object> map = ServiceUtil.getFutureValueLoggingError(serviceInstanceFuture);
		if(map == null)  return null;
		if (!map.containsKey("entityId") || map.get("entityId") == null) {
			LOG.error("Unable to get entityId: {}", map);
		}
        String brooklynEntity = String.valueOf(map.get("entityId"));
        if(!includeEverything) {
            return new BrooklynServiceInstance(serviceInstanceId, String.valueOf(map.get("serviceDefinitionId"))).withEntityId(brooklynEntity);
        }

        String lastOperation = String.valueOf(map.get("operation"));
        String currentBrooklynStatus = ServiceUtil.getFutureValueLoggingError(restAdmin.getServiceState(brooklynEntity));
        if(currentBrooklynStatus == null) {
        	currentBrooklynStatus = "DESTROYED";
        }
        boolean deleted = false;
		OperationState state = nextState(lastOperation, currentBrooklynStatus);

        if(state.equals(OperationState.SUCCEEDED)){
        	if(currentBrooklynStatus.equals("DESTROYED")){
        		delete(serviceInstanceId);
        		deleted = true;
        	}
        }

        BrooklynServiceInstance newInstance = new BrooklynServiceInstance(serviceInstanceId, String.valueOf(map.get("serviceDefinitionId")))
                .withEntityId(brooklynEntity)
                .withOperation(lastOperation)
                .withOperationStatus(state);

        if(!deleted)
        	save(newInstance);
        return newInstance;
	}
	
	public void delete(String serviceInstanceId) {
		restAdmin.deleteConfig(application, entity, serviceInstanceId);
	}

	
	public BrooklynServiceInstance save(BrooklynServiceInstance instance) {
		return (BrooklynServiceInstance) ServiceUtil.getFutureValueLoggingError(restAdmin.setConfig(application, entity, instance.getServiceInstanceId(), instance));
	}
	
	private OperationState nextState(String lastOperation, String currentBrooklynStatus) {
        lastOperation = lastOperation.toUpperCase();
		OperationState nextState = OperationState.FAILED;
		switch(lastOperation){
		case "CREATING":
			if (ImmutableSet.of("CREATED", "STARTING").contains(currentBrooklynStatus)) {
				nextState = OperationState.IN_PROGRESS;
			}

			if (currentBrooklynStatus.equals("RUNNING")) {
				nextState = OperationState.SUCCEEDED;
			}
			break; // all other statuses transition to fail
		case "DELETING":
			if (ImmutableSet.of("STOPPING", "STOPPED").contains(currentBrooklynStatus)) {
				nextState = OperationState.IN_PROGRESS;
			}

			if (currentBrooklynStatus.equals("DESTROYED")) {
				nextState = OperationState.SUCCEEDED;
			}		
		}
		LOG.info("lastOperation={}, currentBrooklynStatus={}, nextState={}", lastOperation, currentBrooklynStatus, nextState);
		return nextState;
	}
}
