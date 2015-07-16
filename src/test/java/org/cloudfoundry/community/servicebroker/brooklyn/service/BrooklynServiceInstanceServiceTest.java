package org.cloudfoundry.community.servicebroker.brooklyn.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.cloudfoundry.community.servicebroker.brooklyn.BrooklynConfiguration;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceRepository;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.fixture.ServiceInstanceFixture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import brooklyn.rest.domain.TaskSummary;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {BrooklynConfiguration.class})
public class BrooklynServiceInstanceServiceTest {
	
	private final static String SVC_INST_ID = "serviceInstanceId";
    private final static int TEST_MIN_CORES = 4;
    private final static int TEST_MIN_RAM = 4096;
	
	@Mock
	private BrooklynRestAdmin admin;
	@Mock
	private ServiceDefinition serviceDefinition;
	@Mock 
	private TaskSummary entity;
	@InjectMocks
	private BrooklynServiceInstanceService service;
	
	@Mock
	private BrooklynServiceInstanceRepository repository;
	@Mock
	private BrooklynCatalogService catalogService;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void newServiceInstanceCreatedSuccessfully() 
			throws ServiceInstanceExistsException, ServiceBrokerException {

		when(admin.createApplication(any(String.class))).thenReturn(entity);
		when(catalogService.getServiceDefinition(any(String.class))).thenReturn(serviceDefinition);
        when(serviceDefinition.getPlans()).thenReturn(ImmutableList.of(new Plan("planId", "test_name", "test_description", ImmutableMap.of("location", "test_location"))));
		
		CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(serviceDefinition.getId(), "planId", "organizationGuid", "spaceGuid");
		ServiceInstance instance = service.createServiceInstance(request.withServiceInstanceId(SVC_INST_ID));
		
		assertNotNull(instance);
		assertEquals(SVC_INST_ID, instance.getServiceInstanceId());
	}
	
	@Test(expected=ServiceInstanceExistsException.class)
	public void serviceInstanceCreationFailsWithExistingInstance()  
			throws ServiceInstanceExistsException, ServiceBrokerException {
		when(repository.findOne(any(String.class))).thenReturn(ServiceInstanceFixture.getServiceInstance());	
		CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(serviceDefinition.getId(), "planId", "organizationGuid", "spaceGuid");
		service.createServiceInstance(request.withServiceInstanceId(SVC_INST_ID));
	}
	
	@Test
	public void serviceInstanceRetrievedSuccessfully() 
			throws ServiceInstanceExistsException, ServiceBrokerException{
		
		when(repository.findOne(any(String.class))).thenReturn(ServiceInstanceFixture.getServiceInstance());
		String serviceInstanceId = ServiceInstanceFixture.getServiceInstance().getServiceInstanceId();
		assertEquals(serviceInstanceId, service.getServiceInstance(serviceInstanceId).getServiceInstanceId());
	}
	
	@Test
	public void serviceInstanceDeletedSuccessfully() 
			throws ServiceInstanceExistsException, ServiceBrokerException {

		when(repository.findOne(any(String.class))).thenReturn(ServiceInstanceFixture.getServiceInstance());
		String instanceId = ServiceInstanceFixture.getServiceInstance().getServiceInstanceId();
		DeleteServiceInstanceRequest request = new DeleteServiceInstanceRequest(instanceId, "serviceId", "planId");
		assertNotNull(service.deleteServiceInstance(request));
		
	}

    @Test
    public void testCreateBlueprint() {
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(serviceDefinition.getId(), "planId", "organizationGuid", "spaceGuid")
                .withServiceInstanceId(SVC_INST_ID);
        when(serviceDefinition.getId()).thenReturn("testService");
        when(serviceDefinition.getPlans()).thenReturn(ImmutableList.of(
                new Plan("planId", "planName", "planDescription", ImmutableMap.of(
                        "location", "testLocation",
                        "provisioning.properties", ImmutableMap.of(
                                "minCores", TEST_MIN_CORES,
                                "minRam", TEST_MIN_RAM
                        )
                ))
        ));
        String expectedBlueprint = String.format("{\"services\":[\"type\": \"%s\"], \"locations\": [\"%s\"], \"brooklyn.config\":{\"provisioning.properties\":{\"minCores\":%d,\"minRam\":%d}}}", serviceDefinition.getId(), "testLocation", TEST_MIN_CORES, TEST_MIN_RAM);
        String blueprint = service.createBlueprint(serviceDefinition, request);
        // Remove whitespace for assertion so we're not tied to the implementation's whitespace rules
        assertEquals(expectedBlueprint.replace(" ", ""), blueprint.replace(" ", ""));

    }
}
