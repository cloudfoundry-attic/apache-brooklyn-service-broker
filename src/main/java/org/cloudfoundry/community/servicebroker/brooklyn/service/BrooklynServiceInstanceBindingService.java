package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.brooklyn.feed.http.JsonFunctions;
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
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

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
        Predicate<String> sensorPredicate= Predicates.alwaysTrue();
        Predicate<String> entityPredicate= Predicates.alwaysTrue();
        Object planYamlObject = service.getMetadata().get("planYaml");
        if (planYamlObject != null) {
            Object rootElement = Iterables.getOnlyElement(Yamls.parseAll(String.valueOf(planYamlObject)));
			if (rootElement instanceof Map) {
				sensorPredicate = getSensorPredicate(rootElement);
				entityPredicate = getEntityPredicate(rootElement);
			}
        }

		if (ServiceUtil.getFutureValueLoggingError(admin.hasEffector(entityId, entityId, "bind"))) {
			Future<String> effector = admin.invokeEffector(entityId, entityId, "bind", "0", ImmutableMap.of());
			String bindResponse = ServiceUtil.getFutureValueLoggingError(effector);
			LOG.info("Calling bind effector: {}", bindResponse);
			String id = (String) Functions.compose(JsonFunctions.getPath("id"), JsonFunctions.asJson()).apply(bindResponse);
			try {
				admin.blockUntilTaskCompletes(id);
			} catch (PollingException e) {
				throw new ServiceBrokerException("could not bind: " + e.getMessage());
			}
		}
        Future<Map<String, Object>> credentialsFuture = admin.getCredentialsFromSensors(entityId, sensorPredicate, entityPredicate);
        Map<String, Object> credentials = ServiceUtil.getFutureValueLoggingError(credentialsFuture);
        serviceInstanceBinding = new ServiceInstanceBinding(request.getBindingId(), request.getServiceInstanceId(), null, null, request.getAppGuid());
		bindingRepository.save(serviceInstanceBinding);
		return new ServiceInstanceBinding(request.getBindingId(), request.getServiceInstanceId(), credentials, null, request.getAppGuid());
	}

    @VisibleForTesting
    public static Predicate<String> getContainsItemInSectionPredicate(Object rootElement, String section) {
        return s -> containsItemInSection(s, (Map<?, ?>) rootElement, section);
    }
    
    private static Boolean containsItemInSection(Object item, Map<?, ?> map, String section){
    	Map<?, ?> brooklynConfig = (Map<?, ?>) map.get("brooklyn.config");
    	Map<?, ?> brokerConfig = (Map<?, ?>) getValue(brooklynConfig, "broker.config");
		List<?> list = (List<?>) getValue(brokerConfig, section);
		return listContains(list, item);
    }
    
    private static Boolean listContains(Object list, Object s) {
    	return list == null ? true : ((List<?>) list).contains(s);
    }
    
    private static Object getValue(Map<?, ?> map, String key){
    	return (map == null || !map.containsKey(key)) ? null : map.get(key);
    }

    @VisibleForTesting
    public static Predicate<String> getSensorPredicate(Object rootElement) {
    	return getContainsItemInSectionPredicate(rootElement, "sensor.whitelist");
    }
    
    public static Predicate<String> getEntityPredicate(Object rootElement){
    	return Predicates.not(getContainsItemInSectionPredicate(rootElement, "entity.blacklist"));
    }


	@Override
	public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request)
			throws ServiceBrokerException {
		
		String bindingId = request.getBindingId();
        ServiceInstanceBinding serviceInstanceBinding = getServiceInstanceBinding(bindingId);
        if (serviceInstanceBinding != null) {
        	ServiceInstance serviceInstance = instanceRepository.findOne(serviceInstanceBinding.getServiceInstanceId(), false);
    		String entityId = serviceInstance.getServiceDefinitionId();
    		Future<String> effector = admin.invokeEffector(entityId, entityId, "unbind", "0", ImmutableMap.of());
    		LOG.info("Calling unbind effector: {}", ServiceUtil.getFutureValueLoggingError(effector));
		    LOG.info("Deleting service binding: [BindingId={}]", bindingId);
			bindingRepository.delete(bindingId);
		}
		return serviceInstanceBinding;
	}

}
