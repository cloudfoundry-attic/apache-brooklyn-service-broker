package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.brooklyn.util.yaml.Yamls;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceBindingRepository;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceRepository;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

@Service
public class BrooklynServiceInstanceBindingService implements
		ServiceInstanceBindingService {
    
    private static final Logger LOG = LoggerFactory.getLogger(BrooklynServiceInstanceBindingService.class);
    
	private BrooklynRestAdmin admin;
	private BrooklynServiceInstanceBindingRepository bindingRepository;
    private BrooklynServiceInstanceRepository instanceRepository;
    private BrooklynCatalogService catalogService;

    @Autowired
	public BrooklynServiceInstanceBindingService(BrooklynRestAdmin admin, BrooklynServiceInstanceBindingRepository bindingRepository, BrooklynServiceInstanceRepository instanceRepository, BrooklynCatalogService catalogService) {
		this.admin = admin;
		this.bindingRepository = bindingRepository;
        this.instanceRepository = instanceRepository;
        this.catalogService = catalogService;
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
		
		ServiceInstance serviceInstance = instanceRepository.findOne(request.getServiceInstanceId(), false);
		String entityId = serviceInstance.getServiceDefinitionId();
		
		LOG.info("creating service binding: [entity={}, serviceDefinitionId={}, bindingId={}, serviceInstanceId={}, appGuid={}", 
		      entityId, request.getServiceDefinitionId(), request.getBindingId(), request.getServiceInstanceId(), request.getAppGuid()
        );

        ServiceDefinition service = catalogService.getServiceDefinition(request.getServiceDefinitionId());
        Predicate<String> sensorPredicate;
        Predicate<String> entityPredicate;
        Object planYamlObject = service.getMetadata().get("planYaml");
        if (planYamlObject == null) {
            sensorPredicate = Predicates.alwaysTrue();
            entityPredicate = Predicates.alwaysTrue();
        } else {
            String planYaml = String.valueOf(planYamlObject);
            sensorPredicate = getSensorPredicate(planYaml);
            entityPredicate = getEntityPredicate(planYaml);
        }

        Future<Map<String, Object>> credentialsFuture = admin.getCredentialsFromSensors(entityId, sensorPredicate, entityPredicate);
        Map<String, Object> credentials = ServiceUtil.getFutureValueLoggingError(credentialsFuture);
        serviceInstanceBinding = new ServiceInstanceBinding(request.getBindingId(), request.getServiceInstanceId(), null, null, request.getAppGuid());
		bindingRepository.save(serviceInstanceBinding);
		return new ServiceInstanceBinding(request.getBindingId(), request.getServiceInstanceId(), credentials, null, request.getAppGuid());
	}
	
    @VisibleForTesting
    public static Predicate<String> getPredicate(String planYaml, String section) {
        return s -> {
            Iterator<Object> iterator = Yamls.parseAll(planYaml).iterator();
            while (iterator.hasNext()) {
                Object next = iterator.next();
                if (next instanceof Map){
                    Map<String, Object> map = (Map<String, Object>) next;
                    return getAndTransform(map, "brooklyn.config", brooklynConfig ->
                        getAndTransform((Map<String, Object>) map.get("brooklyn.config"), "broker.config", brokerConfig ->
                            getAndTransform((Map<String, Object>) brokerConfig, section, list ->
                            list == null ? true : ((List<String>) list).contains(s))
                        )
                    );
                }
            }
            return true;
        };
    }

    @VisibleForTesting
    public static Predicate<String> getSensorPredicate(String planYaml) {
    	return getPredicate(planYaml, "sensor.whitelist");
    }
    
    public static Predicate<String> getEntityPredicate(String planYaml){
    	return Predicates.not(getPredicate(planYaml, "entity.blacklist"));
    }

    private static <T> T getAndTransform(Map<String, Object> map, String key, Function<Object, T> transformer) {
        if (map == null || !map.containsKey(key)) {
            return transformer.apply(null);
        }
        return transformer.apply(map.get(key));
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
