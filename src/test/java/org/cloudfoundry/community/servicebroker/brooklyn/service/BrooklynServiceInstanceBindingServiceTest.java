package org.cloudfoundry.community.servicebroker.brooklyn.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.apache.brooklyn.rest.client.BrooklynApi;
import org.cloudfoundry.community.servicebroker.brooklyn.BrooklynConfiguration;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceBindingRepository;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceRepository;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.model.fixture.ServiceInstanceBindingFixture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import brooklyn.rest.api.EntityApi;
import brooklyn.rest.api.SensorApi;
import brooklyn.rest.domain.EntitySummary;
import brooklyn.rest.domain.SensorSummary;
import brooklyn.util.ResourceUtils;

import com.google.api.client.util.Maps;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {BrooklynConfiguration.class})
public class BrooklynServiceInstanceBindingServiceTest {

private final static String SVC_INST_BIND_ID = "serviceInstanceBindingId";
    private static final String WHITELIST_YAML = ResourceUtils.create().getResourceAsString("whitelist-catalog.yaml");
    private static final String NO_PLANS_YAML = ResourceUtils.create().getResourceAsString("noplans-catalog.yaml");
    private static final String PLANS_YAML = ResourceUtils.create().getResourceAsString("plans-catalog.yaml");

    private static final Map<String, Object> EXPECTED_CREDENTIALS = Maps.newHashMap();

    static {
        Map<String, Object> expectedCredentialsChild = Maps.newHashMap();
        expectedCredentialsChild.put("children", ImmutableMap.of());
        expectedCredentialsChild.put("sensor.one.name", null);

        EXPECTED_CREDENTIALS.put("name", expectedCredentialsChild);
    }

    @Mock
	private BrooklynRestAdmin admin;
	@Mock
	private ServiceInstance serviceInstance;

	private BrooklynServiceInstanceBindingService bindingService;
	
	@Mock
	private BrooklynServiceInstanceBindingRepository bindingRepository;
	@Mock
	private BrooklynServiceInstanceRepository instanceRepository;
    @Mock
    private BrooklynCatalogService brooklynCatalogService;
    @Mock
    private ServiceDefinition serviceDefinition;
    @Mock
    private SensorApi sensorApi;
    @Mock
    private BrooklynApi brooklynApi;
    @Mock
    private EntityApi entityApi;

    @Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		bindingService = new BrooklynServiceInstanceBindingService(admin, bindingRepository, instanceRepository, brooklynCatalogService);
	}
	
	@Test
	public void newServiceInstanceBindingCreatedSuccessfully() 
			throws ServiceBrokerException, ServiceInstanceBindingExistsException {

		when(admin.getCredentialsFromSensors(any(String.class), any(Predicate.class))).thenReturn(new AsyncResult<>(Collections.<String, Object>emptyMap()));
		when(instanceRepository.findOne(any(String.class))).thenReturn(serviceInstance);
        when(serviceDefinition.getMetadata()).thenReturn(ImmutableMap.of());
        when(brooklynCatalogService.getServiceDefinition(Mockito.anyString())).thenReturn(serviceDefinition);
		CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest(serviceInstance.getServiceDefinitionId(), "planId", "appGuid");
		ServiceInstanceBinding binding = bindingService.createServiceInstanceBinding(request.withBindingId(SVC_INST_BIND_ID));
		
		assertNotNull(binding);
		assertEquals(SVC_INST_BIND_ID, binding.getId());
	}

    @Test
    public void testWhitelistCreatedSuccessfully() throws ServiceInstanceBindingExistsException, ServiceBrokerException {

        bindingService = new BrooklynServiceInstanceBindingService(new BrooklynRestAdmin(brooklynApi), bindingRepository, instanceRepository, brooklynCatalogService);

        when(admin.getCredentialsFromSensors(Mockito.anyString(), Mockito.any(Predicate.class))).thenCallRealMethod();

        when(brooklynApi.getSensorApi()).thenReturn(sensorApi);
        when(sensorApi.list(Mockito.anyString(), Mockito.anyString())).thenReturn(ImmutableList.of(
                new SensorSummary("my.sensor", "my sensor type", "my sensor description", ImmutableMap.of()),
                new SensorSummary("sensor.one.name", "sensor one type", "sensor one description", ImmutableMap.of())
        ));
        when(brooklynApi.getEntityApi()).thenReturn(entityApi);
        when(entityApi.list(Mockito.any())).thenReturn(ImmutableList.of(
                new EntitySummary("entityId", "name", "entityType", "catalogItemId", ImmutableMap.of())
        ));
        when(instanceRepository.findOne(any(String.class))).thenReturn(serviceInstance);
        when(brooklynCatalogService.getServiceDefinition(Mockito.anyString())).thenReturn(serviceDefinition);
        when(serviceInstance.getServiceDefinitionId()).thenReturn("serviceDefinitionId");
        when(serviceDefinition.getMetadata()).thenReturn(ImmutableMap.of("planYaml", WHITELIST_YAML));
        CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest(serviceInstance.getServiceDefinitionId(), "planId", "appGuid");
        ServiceInstanceBinding binding = bindingService.createServiceInstanceBinding(request.withBindingId(SVC_INST_BIND_ID));

        ServiceInstanceBinding expectedBinding = new ServiceInstanceBinding(SVC_INST_BIND_ID, serviceInstance.getServiceInstanceId(), EXPECTED_CREDENTIALS, null, "appGuid");

        assertEquals(expectedBinding.getAppGuid(), binding.getAppGuid());
        assertEquals(expectedBinding.getCredentials(), binding.getCredentials());
        assertEquals(expectedBinding.getId(), binding.getId());
        assertEquals(expectedBinding.getServiceInstanceId(), binding.getServiceInstanceId());
        assertEquals(expectedBinding.getSyslogDrainUrl(), binding.getSyslogDrainUrl());
    }
	
	@Test(expected=ServiceInstanceBindingExistsException.class)
	public void serviceInstanceCreationFailsWithExistingInstance()  
			throws ServiceBrokerException, ServiceInstanceBindingExistsException {
		
		when(bindingRepository.findOne(any(String.class)))
		.thenReturn(ServiceInstanceBindingFixture.getServiceInstanceBinding());	
		when(admin.getApplicationSensors(any(String.class))).thenReturn(new AsyncResult<>(Collections.<String, Object>emptyMap()));
		CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest(serviceInstance.getServiceDefinitionId(), "planId", "appGuid");
				
		bindingService.createServiceInstanceBinding(request.withBindingId(SVC_INST_BIND_ID));
		bindingService.createServiceInstanceBinding(request.withBindingId(SVC_INST_BIND_ID));
	}
	
	@Test
	public void serviceInstanceBindingRetrievedSuccessfully() 
			throws ServiceBrokerException, ServiceInstanceBindingExistsException{

		ServiceInstanceBinding binding = ServiceInstanceBindingFixture.getServiceInstanceBinding();
		when(bindingRepository.findOne(any(String.class))).thenReturn(binding);
		
		assertEquals(binding.getId(), bindingService.getServiceInstanceBinding(binding.getId()).getId());
	}
	
	@Test
	public void serviceInstanceBindingDeletedSuccessfully() 
			throws ServiceBrokerException, ServiceInstanceBindingExistsException {
		
		ServiceInstanceBinding binding = ServiceInstanceBindingFixture.getServiceInstanceBinding();
		when(bindingRepository.findOne(any(String.class))).thenReturn(binding);

		DeleteServiceInstanceBindingRequest request = new DeleteServiceInstanceBindingRequest(binding.getId(), serviceInstance, "serviceId", "planId");
		assertNotNull(bindingService.deleteServiceInstanceBinding(request));
	}

    @Test
    public void testGetSensorPredicate() {
        Predicate<String> predicate = BrooklynServiceInstanceBindingService.getSensorPredicate(WHITELIST_YAML);
        assertNotNull(predicate);
        assertTrue(predicate.apply("foo.bar"));
        assertFalse(predicate.apply("bar.foo"));
    }

    @Test
    public void testGetSensorPredicateNoPlans() {
        Predicate<String> predicate = BrooklynServiceInstanceBindingService.getSensorPredicate(NO_PLANS_YAML);
        assertNotNull(predicate);
        assertTrue(predicate.apply("foo.bar"));
        assertTrue(predicate.apply("bar.foo"));
    }

    @Test
    public void testGetSensorPredicateNoWhitelist() {
        Predicate<String> predicate = BrooklynServiceInstanceBindingService.getSensorPredicate(PLANS_YAML);
        assertNotNull(predicate);
        assertTrue(predicate.apply("foo.bar"));
        assertTrue(predicate.apply("bar.foo"));
    }
}
