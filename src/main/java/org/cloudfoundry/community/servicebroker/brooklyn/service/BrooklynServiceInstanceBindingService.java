package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import org.apache.brooklyn.feed.http.JsonFunctions;
import org.apache.brooklyn.util.yaml.Yamls;
import org.cloudfoundry.community.servicebroker.brooklyn.model.BrooklynServiceInstance;
import org.cloudfoundry.community.servicebroker.brooklyn.model.BrooklynServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceBindingRepository;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

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

   protected BrooklynServiceInstanceBinding getServiceInstanceBinding(String bindingId) {
      return bindingRepository.findOne(bindingId);
   }

   @Override
   public CreateServiceInstanceBindingResponse createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {

      BrooklynServiceInstanceBinding serviceInstanceBinding = getServiceInstanceBinding(request.getBindingId());
      if (serviceInstanceBinding != null) {
         throw new ServiceInstanceBindingExistsException(serviceInstanceBinding.getServiceInstanceId(), request.getBindingId());
      }

      BrooklynServiceInstance serviceInstance = instanceRepository.findOne(request.getServiceInstanceId(), false);
      String entityId = serviceInstance.getEntityId();

      LOG.info("creating service binding: [entity={}, serviceDefinitionId={}, bindingId={}, serviceInstanceId={}, appGuid={}",
              entityId, request.getServiceDefinitionId(), request.getBindingId(), request.getServiceInstanceId(), request.getAppGuid()
      );

      ServiceDefinition service = catalogService.getServiceDefinition(request.getServiceDefinitionId());
      Predicate<String> sensorWhitelistPredicate = x -> true;
      Predicate<String> entityBlacklistPredicate = x -> true;
      Predicate<String> sensorBlacklistPredicate = x -> true;
      Predicate<String> entityWhitelistPredicate = x -> true;
      Object planYamlObject = service.getMetadata().get("planYaml");
      if (planYamlObject != null) {
         Object rootElement = Iterables.getOnlyElement(Yamls.parseAll(String.valueOf(planYamlObject)));
         if (rootElement instanceof Map) {
            sensorWhitelistPredicate = getSensorWhitelistPredicate(rootElement);
            sensorBlacklistPredicate = getSensorBlacklistPredicate(rootElement);
            entityWhitelistPredicate = getEntityWhitelistPredicate(rootElement);
            entityBlacklistPredicate = getEntityBlacklistPredicate(rootElement);
         }
      }

      String childEntityId = null;
      String bindResponse;
      Map<String, Object> parameters = request.getParameters() != null ? request.getParameters() : ImmutableMap.of();
      if (ServiceUtil.getFutureValueLoggingError(admin.hasEffector(entityId, entityId, "bind"))) {
         Future<String> effector = admin.invokeEffector(entityId, entityId, "bind", "never", parameters);
         bindResponse = ServiceUtil.getFutureValueLoggingError(effector);
         if (bindResponse == null) {
            throw new RuntimeException(String.format("cannot invoke bind effector on entity %s with %s", entityId, Iterables.toString(request.getParameters().entrySet())));
         }
         LOG.info("calling bind effector on entity {} with {}: {}", entityId, Iterables.toString(parameters.entrySet()), bindResponse);
         JsonElement jsonElement = JsonFunctions.asJson().apply(bindResponse);
         if (jsonElement instanceof JsonArray) {
            childEntityId = ((JsonArray) jsonElement).get(0).getAsString();
         } else {
            childEntityId = jsonElement.getAsString();
         }
      }
      Future<Map<String, Object>> credentialsFuture = admin.getCredentialsFromSensors(entityId, MoreObjects.firstNonNull(childEntityId, entityId), sensorWhitelistPredicate, sensorBlacklistPredicate, entityWhitelistPredicate, entityBlacklistPredicate);
      Map<String, Object> credentials = ServiceUtil.getFutureValueLoggingError(credentialsFuture);
      LOG.info("credentials: {}", Iterables.toString(credentials.entrySet()));
      serviceInstanceBinding = new BrooklynServiceInstanceBinding(request.getBindingId(), request.getServiceInstanceId(), null, request.getAppGuid(), childEntityId);
      bindingRepository.save(serviceInstanceBinding);
      return new CreateServiceInstanceAppBindingResponse().withCredentials(credentials);
   }

   @VisibleForTesting
   public static Predicate<String> getContainsItemInSectionPredicate(Object rootElement, String section, boolean ifAbsent) {
      return s -> containsItemInSection(s, (Map<?, ?>) rootElement, section, ifAbsent);
   }

   private static Boolean containsItemInSection(Object item, Map<?, ?> map, String section, boolean ifAbsent) {
      Map<?, ?> brooklynConfig = (Map<?, ?>) map.get("brooklyn.config");
      Map<?, ?> brokerConfig = (Map<?, ?>) getValue(brooklynConfig, "broker.config");
      List<?> list = (List<?>) getValue(brokerConfig, section);
      if (list == null) return ifAbsent;
      return list.contains(item);
   }

   private static Object getValue(Map<?, ?> map, String key) {
      return (map == null || !map.containsKey(key)) ? null : map.get(key);
   }

   @VisibleForTesting
   public static Predicate<String> getSensorWhitelistPredicate(Object rootElement) {
      return getContainsItemInSectionPredicate(rootElement, "sensor.whitelist", true);
   }

   @VisibleForTesting
   public static Predicate<String> getSensorBlacklistPredicate(Object rootElement) {
      return getContainsItemInSectionPredicate(rootElement, "sensor.blacklist", false).negate();
   }

   @VisibleForTesting
   public static Predicate<String> getEntityWhitelistPredicate(Object rootElement) {
      return getContainsItemInSectionPredicate(rootElement, "entity.whitelist", true);
   }

   @VisibleForTesting
   public static Predicate<String> getEntityBlacklistPredicate(Object rootElement) {
      return getContainsItemInSectionPredicate(rootElement, "entity.blacklist", false).negate();
   }

   @Override
   public void deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request)
           throws ServiceBrokerException {

      String bindingId = request.getBindingId();
      BrooklynServiceInstanceBinding serviceInstanceBinding = getServiceInstanceBinding(bindingId);
      if (serviceInstanceBinding != null) {
         BrooklynServiceInstance serviceInstance = instanceRepository.findOne(serviceInstanceBinding.getServiceInstanceId(), false);
         String appId = serviceInstance.getEntityId();
         String entityId = serviceInstanceBinding.getEntityId();
         if (entityId == null) entityId = appId;
         Future<String> effector = admin.invokeEffector(appId, entityId, "unbind", "0", ImmutableMap.<String, Object>of(
                 "app_guid", serviceInstanceBinding.getAppGuid()
         ));
         LOG.info("Calling unbind effector: {}", ServiceUtil.getFutureValueLoggingError(effector));
         LOG.info("Deleting service binding: [BindingId={}]", bindingId);
         bindingRepository.delete(bindingId);
      }
   }

}
