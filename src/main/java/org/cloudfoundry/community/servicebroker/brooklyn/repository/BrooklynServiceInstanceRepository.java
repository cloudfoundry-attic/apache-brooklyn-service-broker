package org.cloudfoundry.community.servicebroker.brooklyn.repository;

import java.util.Map;

import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.OperationState;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceLastOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import brooklyn.rest.client.BrooklynApi;

import com.google.common.collect.ImmutableMap;

@Service
public class BrooklynServiceInstanceRepository {
	
	private static final Logger LOG = LoggerFactory.getLogger(BrooklynServiceInstanceRepository.class);

	@Autowired
	private BrooklynApi restApi;
	private String application = "service-broker-records";
	private String entity = "service-instance-repository";

    private final Map<String, OperationState> SERVICE_STATE_TO_OPERATION_STATE = ImmutableMap.<String, OperationState>builder()
            .put("CREATED", OperationState.IN_PROGRESS)
            .put("STARTING", OperationState.IN_PROGRESS)
            .put("RUNNING", OperationState.SUCCEEDED)
            .put("STOPPING", OperationState.IN_PROGRESS)
            .put("STOPPED", OperationState.IN_PROGRESS)
            .put("DESTROYED", OperationState.IN_PROGRESS)
            .put("ON_FIRE", OperationState.FAILED)
            .build();

	@SuppressWarnings("unchecked")
	public ServiceInstance findOne(String serviceInstanceId) {
		Object object;
		try{
			object = restApi.getEntityConfigApi().get(application, entity, serviceInstanceId, false);
		}catch(Exception e){
			LOG.error("Unable to get instance with serviceInstanceId={}", serviceInstanceId);
			return null;
		}
		
		if (object == null || !(object instanceof Map)) {
			LOG.error("Unable to get instance with serviceInstanceId={}", serviceInstanceId);
			return null;
		}
		Map<String, Object> map = (Map<String, Object>) object;
		if (!map.containsKey("serviceDefinitionId") || map.get("serviceDefinitionId") == null) {
			LOG.error("Unable to get serviceDefinitionId: {}", map);
		}
		
        String brooklynEntity = String.valueOf(map.get("serviceDefinitionId"));
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(
                brooklynEntity,
				map.get("planId").toString(),
				map.get("organizationGuid").toString(),
				map.get("spaceGuid").toString(),
                true
				);
        Map<String, Object> previousOperationMap = (Map<String, Object>)map.get("serviceInstanceLastOperation");
        String previousOperation = previousOperationMap.get("description").toString();
        // FIXME: this throws an exception as Brooklyn gives 404
        String serviceState;
        OperationState state;
        boolean deleted = false;
        try {
            serviceState = String.valueOf(restApi.getSensorApi().get(brooklynEntity, brooklynEntity, "service.state", false));
            state = SERVICE_STATE_TO_OPERATION_STATE.get(serviceState);
        } catch (Exception e) {
            // Entity is no longer managed by Brooklyn
            LOG.debug(e.getMessage());
            serviceState = "DESTROYED";
            if (Operations.DELETING.equals(previousOperation)) {
                deleted = true;
                state = OperationState.SUCCEEDED;
                delete(serviceInstanceId);
            } else if (Operations.CREATING.equals(previousOperation)) {
                state = OperationState.FAILED;
            } else {
                LOG.error("Unexpected previous operation: " + previousOperation);
                return null;
            }
        }

        ServiceInstanceLastOperation lastOp = new ServiceInstanceLastOperation(serviceState, state);
        ServiceInstance newInstance = new ServiceInstance(request.withServiceInstanceId(serviceInstanceId))
                .withLastOperation(lastOp)
                .isAsync(true);

        if (!deleted) {
            save(newInstance);
        }
        return newInstance;
	}
	
	public void delete(String serviceInstanceId) {
		try{
			restApi.getEntityConfigApi().set(application, entity, serviceInstanceId, false, "");
		} catch(Exception e){
			LOG.error("unable to delete {} {}", serviceInstanceId, e);
		}
	}

	
	public <S extends ServiceInstance> S save(S instance) {
		try{
			restApi.getEntityConfigApi().set(application, entity, instance.getServiceInstanceId(), false, instance);
			return instance;
		} catch(Exception e){
			LOG.error("unable to save {} {}", instance, e);
			return null;
		}
	}
}
