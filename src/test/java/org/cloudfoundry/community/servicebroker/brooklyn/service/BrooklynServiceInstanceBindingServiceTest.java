package org.cloudfoundry.community.servicebroker.brooklyn.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.brooklyn.rest.api.ActivityApi;
import org.apache.brooklyn.rest.api.EffectorApi;
import org.apache.brooklyn.rest.api.EntityApi;
import org.apache.brooklyn.rest.api.SensorApi;
import org.apache.brooklyn.rest.client.BrooklynApi;
import org.apache.brooklyn.rest.domain.EntitySummary;
import org.apache.brooklyn.rest.domain.SensorSummary;
import org.apache.brooklyn.rest.domain.TaskSummary;
import org.apache.brooklyn.util.core.ResourceUtils;
import org.apache.brooklyn.util.time.Duration;
import org.apache.brooklyn.util.yaml.Yamls;
import org.apache.http.client.HttpClient;
import org.cloudfoundry.community.servicebroker.brooklyn.BrooklynConfiguration;
import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
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

import com.google.api.client.util.Maps;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {BrooklynConfiguration.class})
public class BrooklynServiceInstanceBindingServiceTest {

	private final static String SVC_INST_BIND_ID = "serviceInstanceBindingId";
	private final static String SVC_DEFN_ID = "serviceDefinitionId";
    private static final String WHITELIST_YAML = ResourceUtils.create().getResourceAsString("whitelist-catalog.yaml");
    private static final String NO_PLANS_YAML = ResourceUtils.create().getResourceAsString("noplans-catalog.yaml");
    private static final String PLANS_YAML = ResourceUtils.create().getResourceAsString("plans-catalog.yaml");

    private static final Map<String, Object> EXPECTED_CREDENTIALS = Maps.newHashMap();

    static {
        Map<String, Object> expectedCredentialsChild = Maps.newHashMap();
        expectedCredentialsChild.put("sensor.one.name", null);

        EXPECTED_CREDENTIALS.putAll(expectedCredentialsChild);
    }

    private final String TASK_ID = "NrBavIWX";
    private final String TASK_RESPONSE_INCOMPLETE = "{id:\"" + TASK_ID + "\",displayName:\"pre-install\",description:\"\",entityId:\"Mo6NM5Qt\",entityDisplayName:\"MongoDBServer:Mo6N\",tags:[{wrappingType:\"contextEntity\",entity:{type:\"org.apache.brooklyn.api.entity.Entity\",id:\"Mo6NM5Qt\"}},{entitlementContext:{user:\"admin\",sourceIp:\"0:0:0:0:0:0:0:1\",requestUri:\"/v1/applications\",requestUniqueIdentifier:\"XbbETK\"}},\"SUB-TASK\"],submitTimeUtc:1443530180701,startTimeUtc:1443530180701,endTimeUtc:1443530180724,currentStatus:\"Completed\",result:null,isError:false,isCancelled:false,children:[],submittedByTask:{link:\"/v1/activities/TBCqTO6X\",metadata:{id:\"TBCqTO6X\",taskName:\"start (processes)\",entityId:\"Mo6NM5Qt\",entityDisplayName:\"MongoDBServer:Mo6N\"}},detailedStatus:\"Completed after 23ms No return value (null)\",streams:{},links:{self:\"/v1/activities/UfyAm4ul\",children:\"/v1/activities/UfyAm4ul/children\",entity:\"/v1/applications/TijtVDIn/entities/Mo6NM5Qt\"}}";
    private final TaskSummary TASK_SUMMARY_INCOMPLETE = new TaskSummary(TASK_ID, "displayName", "description", "entityId", "entityDisplayName",
            ImmutableSet.of(), 0l, 0l, null, "currentStatus", "result", false, false,
            ImmutableList.of(), null, null, "blockingDetails", "detailedStatus", ImmutableMap.of(), ImmutableMap.of());
    private final TaskSummary TASK_SUMMARY_COMPLETE = new TaskSummary(TASK_ID, "displayName", "description", "entityId", "entityDisplayName",
            ImmutableSet.of(), 0l, 0l, 1l, "currentStatus", "result", false, false,
            ImmutableList.of(), null, null, "blockingDetails", "detailedStatus", ImmutableMap.of(), ImmutableMap.of());

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
    @Mock
    private HttpClient httpClient;
    @Mock
    private BrooklynConfig config;
    @Mock
    private EffectorApi effectorApi;
    @Mock
    private Response bindEffectorResponse;
    @Mock
    private ActivityApi activityApi;

    @Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		bindingService = new BrooklynServiceInstanceBindingService(admin, bindingRepository, instanceRepository, brooklynCatalogService);
	}
	
	@Test
	public void newServiceInstanceBindingCreatedSuccessfully() 
			throws ServiceBrokerException, ServiceInstanceBindingExistsException {

		when(admin.getCredentialsFromSensors(anyString(), any(Predicate.class), any(Predicate.class))).thenReturn(new AsyncResult<>(Collections.<String, Object>emptyMap()));
        when(admin.hasEffector(anyString(), anyString(), anyString())).thenReturn(new AsyncResult<>(false));
        when(instanceRepository.findOne(anyString(), anyBoolean())).thenReturn(serviceInstance);
        when(serviceDefinition.getMetadata()).thenReturn(ImmutableMap.of());
        when(brooklynCatalogService.getServiceDefinition(anyString())).thenReturn(serviceDefinition);
		CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest(serviceInstance.getServiceDefinitionId(), "planId", "appGuid");
		ServiceInstanceBinding binding = bindingService.createServiceInstanceBinding(request.withBindingId(SVC_INST_BIND_ID));
		
		assertNotNull(binding);
		assertEquals(SVC_INST_BIND_ID, binding.getId());
	}

    @Test
    public void newServiceInstanceBindingCreatedSuccessfullyWithBindEffector()
            throws ServiceBrokerException, ServiceInstanceBindingExistsException, PollingException {
        when(admin.getRestApi()).thenReturn(brooklynApi);
        when(admin.getCredentialsFromSensors(anyString(), any(Predicate.class), any(Predicate.class))).thenReturn(new AsyncResult<>(Collections.<String, Object>emptyMap()));
        when(admin.hasEffector(anyString(), anyString(), anyString())).thenReturn(new AsyncResult<>(true));
        when(admin.invokeEffector(anyString(), anyString(), anyString(), anyString(), anyMap())).thenReturn(new AsyncResult<>(TASK_RESPONSE_INCOMPLETE));
        when(brooklynApi.getActivityApi()).thenReturn(activityApi);
        when(activityApi.get(anyString()))
                .thenReturn(TASK_SUMMARY_INCOMPLETE)
                .thenReturn(TASK_SUMMARY_INCOMPLETE)
                .thenReturn(TASK_SUMMARY_INCOMPLETE)
                .thenReturn(TASK_SUMMARY_INCOMPLETE)
                .thenReturn(TASK_SUMMARY_COMPLETE);
        doCallRealMethod().when(admin).blockUntilTaskCompletes(anyString());
        doCallRealMethod().when(admin).blockUntilTaskCompletes(anyString(), any(Duration.class));
        when(instanceRepository.findOne(anyString(), anyBoolean())).thenReturn(serviceInstance);
        when(serviceDefinition.getMetadata()).thenReturn(ImmutableMap.of());
        when(brooklynCatalogService.getServiceDefinition(anyString())).thenReturn(serviceDefinition);
        CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest(serviceInstance.getServiceDefinitionId(), "planId", "appGuid");
        ServiceInstanceBinding binding = bindingService.createServiceInstanceBinding(request.withBindingId(SVC_INST_BIND_ID));

        assertNotNull(binding);
        assertEquals(SVC_INST_BIND_ID, binding.getId());
    }

    @Test
    public void testWhitelistCreatedSuccessfully() throws ServiceInstanceBindingExistsException, ServiceBrokerException {

        bindingService = new BrooklynServiceInstanceBindingService(new BrooklynRestAdmin(brooklynApi, httpClient, config), bindingRepository, instanceRepository, brooklynCatalogService);

        when(admin.getCredentialsFromSensors(anyString(), any(Predicate.class), any(Predicate.class))).thenCallRealMethod();

        when(brooklynApi.getSensorApi()).thenReturn(sensorApi);
        when(sensorApi.list(anyString(), anyString())).thenReturn(ImmutableList.of(
                new SensorSummary("my.sensor", "my sensor type", "my sensor description", ImmutableMap.of()),
                new SensorSummary("sensor.one.name", "sensor one type", "sensor one description", ImmutableMap.of())
        ));
        when(brooklynApi.getEntityApi()).thenReturn(entityApi);
        when(entityApi.list(any())).thenReturn(ImmutableList.of(
                new EntitySummary("entityId", "name", "entityType", "catalogItemId", ImmutableMap.of())
        ));
        when(instanceRepository.findOne(anyString(), anyBoolean())).thenReturn(serviceInstance);
        when(brooklynCatalogService.getServiceDefinition(Mockito.anyString())).thenReturn(serviceDefinition);
        when(serviceInstance.getServiceDefinitionId()).thenReturn(SVC_DEFN_ID);
        when(serviceDefinition.getMetadata()).thenReturn(ImmutableMap.of("planYaml", WHITELIST_YAML));
        when(brooklynApi.getEffectorApi()).thenReturn(effectorApi);
        when(effectorApi.invoke(anyString(), anyString(), anyString(), anyString(), anyMap())).thenReturn(bindEffectorResponse);

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
		
		when(bindingRepository.findOne(anyString()))
		.thenReturn(ServiceInstanceBindingFixture.getServiceInstanceBinding());	
		when(admin.getApplicationSensors(anyString())).thenReturn(new AsyncResult<>(Collections.<String, Object>emptyMap()));
		CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest(serviceInstance.getServiceDefinitionId(), "planId", "appGuid");
				
		bindingService.createServiceInstanceBinding(request.withBindingId(SVC_INST_BIND_ID));
		bindingService.createServiceInstanceBinding(request.withBindingId(SVC_INST_BIND_ID));
	}
	
	@Test
	public void serviceInstanceBindingRetrievedSuccessfully() 
			throws ServiceBrokerException, ServiceInstanceBindingExistsException{

		ServiceInstanceBinding binding = ServiceInstanceBindingFixture.getServiceInstanceBinding();
		when(bindingRepository.findOne(anyString())).thenReturn(binding);
		
		assertEquals(binding.getId(), bindingService.getServiceInstanceBinding(binding.getId()).getId());
	}
	
	@Test
	public void serviceInstanceBindingDeletedSuccessfully() 
			throws ServiceBrokerException, ServiceInstanceBindingExistsException {
		
		ServiceInstanceBinding binding = ServiceInstanceBindingFixture.getServiceInstanceBinding();
		when(bindingRepository.findOne(anyString())).thenReturn(binding);
		when(instanceRepository.findOne(anyString(), any(Boolean.class))).thenReturn(serviceInstance);
		when(serviceInstance.getServiceDefinitionId()).thenReturn(SVC_DEFN_ID);
		when(admin.invokeEffector(anyString(), anyString(), anyString(), anyString(), anyMap())).thenReturn(new AsyncResult<String>("effector called"));

		DeleteServiceInstanceBindingRequest request = new DeleteServiceInstanceBindingRequest(binding.getId(), serviceInstance, "serviceId", "planId");
		assertNotNull(bindingService.deleteServiceInstanceBinding(request));
	}

    @Test
    public void testGetSensorPredicate() {
    	Object rootElement = Iterables.getOnlyElement(Yamls.parseAll(WHITELIST_YAML));
        Predicate<String> predicate = BrooklynServiceInstanceBindingService.getSensorPredicate(rootElement);
        assertNotNull(predicate);
        assertTrue(predicate.apply("foo.bar"));
        assertFalse(predicate.apply("bar.foo"));
    }

    @Test
    public void testGetSensorPredicateNoPlans() {
    	Object rootElement = Iterables.getOnlyElement(Yamls.parseAll(NO_PLANS_YAML));
        Predicate<String> predicate = BrooklynServiceInstanceBindingService.getSensorPredicate(rootElement);
        assertNotNull(predicate);
        assertTrue(predicate.apply("foo.bar"));
        assertTrue(predicate.apply("bar.foo"));
    }

    @Test
    public void testGetSensorPredicateNoWhitelist() {
    	Object rootElement = Iterables.getOnlyElement(Yamls.parseAll(PLANS_YAML));
        Predicate<String> predicate = BrooklynServiceInstanceBindingService.getSensorPredicate(rootElement);
        assertNotNull(predicate);
        assertTrue(predicate.apply("foo.bar"));
        assertTrue(predicate.apply("bar.foo"));
    }
}
