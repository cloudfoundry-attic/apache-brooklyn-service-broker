package org.cloudfoundry.community.servicebroker.brooklyn.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.brooklyn.rest.domain.EntitySummary;
import org.apache.brooklyn.rest.domain.TaskSummary;
import org.cloudfoundry.community.servicebroker.brooklyn.BrooklynConfiguration;
import org.cloudfoundry.community.servicebroker.brooklyn.model.BrooklynServiceInstance;
import org.cloudfoundry.community.servicebroker.brooklyn.model.DefaultBlueprintPlan;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceExistsException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.Plan;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.UpdateServiceInstanceResponse;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {BrooklynConfiguration.class})
public class BrooklynServiceInstanceServiceTest {
	
	private final static String SVC_INST_ID = "serviceInstanceId";
    private final static String SVC_DEFINITION_ID = "serviceDefinitionId";
    private final static int TEST_MIN_CORES = 4;
    private final static int TEST_MIN_RAM = 4096;

	private static final String SVC_PLAN_ID = "planId";


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
	@Mock
	private EntitySummary entitySummary;
	private BrooklynServiceInstance testServiceInstance;

	@Before
	public void setup() {
		testServiceInstance = new BrooklynServiceInstance(SVC_INST_ID, SVC_DEFINITION_ID).withPlanId(SVC_PLAN_ID).withEntityId("test");
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void newServiceInstanceCreatedSuccessfully() 
			throws ServiceInstanceExistsException, ServiceBrokerException {

		when(admin.createApplication(any(String.class))).thenReturn(new AsyncResult<>(entity));
		when(catalogService.getServiceDefinition(any(String.class))).thenReturn(serviceDefinition);
        when(serviceDefinition.getPlans()).thenReturn(ImmutableList.of(new DefaultBlueprintPlan("planId", "test_name", "test_description","Test App", ImmutableMap.of("location", "test_location"))));
        when(serviceDefinition.getId()).thenReturn(SVC_DEFINITION_ID);
        when(admin.getDashboardUrl(any(String.class))).thenReturn(new AsyncResult<>(null));

		CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(serviceDefinition.getId(), "planId", "organizationGuid", "spaceGuid");
		CreateServiceInstanceResponse instance = service.createServiceInstance(request.withServiceInstanceId(SVC_INST_ID));
		
		assertNotNull(instance);
		// TODO: assert service instance created successfully
		// assertEquals(SVC_INST_ID, instance.getServiceInstanceId());
	}
	
	@Test(expected=ServiceInstanceExistsException.class)
	public void serviceInstanceCreationFailsWithExistingInstance()  
			throws ServiceInstanceExistsException, ServiceBrokerException {
		when(repository.findOne(any(String.class))).thenReturn(testServiceInstance);
		CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(serviceDefinition.getId(), "planId", "organizationGuid", "spaceGuid");
		service.createServiceInstance(request.withServiceInstanceId(SVC_INST_ID));
	}
	
	@Test
	public void serviceInstanceRetrievedSuccessfully() 
			throws ServiceInstanceExistsException, ServiceBrokerException{
		
		when(repository.findOne(any(String.class))).thenReturn(testServiceInstance);
		String serviceInstanceId = testServiceInstance.getServiceInstanceId();
		assertEquals(serviceInstanceId, service.getServiceInstance(serviceInstanceId).getServiceInstanceId());
	}
	
	@Test
	public void serviceInstanceDeletedSuccessfully() 
			throws ServiceInstanceExistsException, ServiceBrokerException {

		when(repository.findOne(any(String.class))).thenReturn(testServiceInstance);
		String instanceId = testServiceInstance.getServiceInstanceId();
		DeleteServiceInstanceRequest request = new DeleteServiceInstanceRequest(instanceId, "serviceId", "planId", null);
		assertNotNull(service.deleteServiceInstance(request));
	}

    @Test
    public void testCreateBlueprintWithProvisioningProperties() {
        when(serviceDefinition.getId()).thenReturn("testService");
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(serviceDefinition.getId(), "planId", "organizationGuid", "spaceGuid", null)
                .withServiceInstanceId(SVC_INST_ID);
        when(serviceDefinition.getPlans()).thenReturn(ImmutableList.of(
                new DefaultBlueprintPlan("planId", "planName", "planDescription","Test App", ImmutableMap.of(
                        "location", "testLocation",
                        "provisioning.properties", ImmutableMap.of(
                                "minCores", TEST_MIN_CORES,
                                "minRam", TEST_MIN_RAM
                        )
                ))
        ));
        when(serviceDefinition.getMetadata()).thenReturn(ImmutableMap.of("brooklynCatalogId", "testService"));
        String expectedBlueprint = String.format("{\"name\":\"Test App (CFService)\",\"services\":[{\"type\": \"%s\", \"id\": \"broker.entity\"}], \"locations\": [\"%s\"], \"brooklyn.config\":{\"provisioning.properties\":{\"minCores\":%d,\"minRam\":%d}}}", serviceDefinition.getId(), "testLocation", TEST_MIN_CORES, TEST_MIN_RAM);
        String blueprint = service.createBlueprint(serviceDefinition, request);
        // Remove whitespace for assertion so we're not tied to the implementation's whitespace rules
        assertEquals(expectedBlueprint.replace(" ", ""), blueprint.replace(" ", ""));
    }

    @Test
    public void testCreateBlueprintWithBrooklynProperties() {
        when(serviceDefinition.getId()).thenReturn("testService");
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(serviceDefinition.getId(), "planId", "organizationGuid", "spaceGuid", null)
                .withServiceInstanceId(SVC_INST_ID);
        when(serviceDefinition.getPlans()).thenReturn(ImmutableList.of(
                new DefaultBlueprintPlan("planId", "planName", "planDescription", "Test App", ImmutableMap.of(
                        "location", "testLocation",
                        "provisioning.properties", ImmutableMap.of(
                                "minCores", TEST_MIN_CORES,
                                "minRam", TEST_MIN_RAM
                        )
                ))
        ));
        when(serviceDefinition.getMetadata()).thenReturn(ImmutableMap.of("brooklynCatalogId", "testService"));
        
        String expectedBlueprint = String.format("{\"name\":\"Test App (CFService)\",\"services\":[{\"type\": \"%s\", \"id\": \"broker.entity\"}], \"locations\": [\"%s\"], \"brooklyn.config\":{\"provisioning.properties\":{\"minCores\":%d,\"minRam\":%d}}}", serviceDefinition.getId(), "testLocation", TEST_MIN_CORES, TEST_MIN_RAM);
        String blueprint = service.createBlueprint(serviceDefinition, request);
        // Remove whitespace for assertion so we're not tied to the implementation's whitespace rules
        assertEquals(expectedBlueprint.replace(" ", ""), blueprint.replace(" ", ""));
    }

    @Test
    public void testCreateBlueprintNoMetadata() {
        when(serviceDefinition.getId()).thenReturn("testService");
        CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(serviceDefinition.getId(), "planId", "organizationGuid", "spaceGuid", null)
                .withServiceInstanceId(SVC_INST_ID);
        when(serviceDefinition.getPlans()).thenReturn(ImmutableList.of(
                new DefaultBlueprintPlan("planId", "planName", "planDescription", "Test App", ImmutableMap.of("location", "testLocation"))
        ));

        when(serviceDefinition.getMetadata()).thenReturn(ImmutableMap.of("brooklynCatalogId", "testService"));
        String expectedBlueprint = String.format("{\"name\":\"Test App (CFService)\",\"services\":[{\"type\": \"%s\", \"id\": \"broker.entity\"}], \"locations\": [\"%s\"]}", serviceDefinition.getId(), "testLocation");
        String blueprint = service.createBlueprint(serviceDefinition, request);
        // Remove whitespace for assertion so we're not tied to the implementation's whitespace rules
        assertEquals(expectedBlueprint.replace(" ", ""), blueprint.replace(" ", ""));
    }

    @Test
	public void testUpdateServiceInstance()
		throws ServiceInstanceExistsException, ServiceBrokerException {
		when(admin.getApplicationDescendents(any(String.class), any(String.class))).thenReturn(new AsyncResult<>(ImmutableList.of(entitySummary)));
		when(admin.hasEffector(any(String.class), any(String.class), any(String.class))).thenReturn(new AsyncResult<Boolean>(true));
		when(repository.findOne(any(String.class))).thenReturn(testServiceInstance);
		when(catalogService.getServiceDefinition(any(String.class))).thenReturn(serviceDefinition);
		when(serviceDefinition.getMetadata()).thenReturn(ImmutableMap.of("brooklynServices", ImmutableList.of(ImmutableMap.of("type", "testType"))));
		when(serviceDefinition.getPlans())
				.thenReturn(ImmutableList.of(
						new DefaultBlueprintPlan("planId",
								"testPlan",
								"test plan description",
								"testplanApp",
								ImmutableMap.of(
										"update", ImmutableList.of(ImmutableMap.of("to", "testPlan2", "effector", ImmutableMap.of("name", "resize", "params", ImmutableMap.of()))))

						),
						new DefaultBlueprintPlan("planId2",
								"testPlan2",
								"test plan description2",
								"testplanApp2",
								ImmutableMap.of(
										"update", ImmutableList.of(ImmutableMap.of("to", "testPlan", "effector", ImmutableMap.of("name", "resize", "params", ImmutableMap.of()))))
						)));

		UpdateServiceInstanceRequest request = new UpdateServiceInstanceRequest(serviceDefinition.getId(), "planId2");

		UpdateServiceInstanceResponse response = service.updateServiceInstance(request);
		assertNotNull(response);

	}

	@Test
	public void testUpdateServiceInstanceFailsWithMap() {
		Plan fromPlan = new DefaultBlueprintPlan("planId",
				"testPlan",
				"test plan description",
				"testplanApp",
				ImmutableMap.of(
						"update",ImmutableMap.of(
						"to","testPlan2",
						"effector",
						ImmutableMap.of(
								"name","resize",
								"params",ImmutableMap.of())))
		);
		Plan toPlan = new DefaultBlueprintPlan("planId2",
				"testPlan2",
				"test plan description",
				"testplanApp",
				ImmutableMap.of(
						"update", ImmutableList.of(
						ImmutableMap.of(
								"to", "testPlan",
								"effector", ImmutableMap.of(
										"name", "resize",
										"params", ImmutableMap.of())))));
		try {
			testDefaultBlueprintPlan(fromPlan, toPlan);
			fail();
		} catch (ServiceInstanceUpdateNotSupportedException cs) {
			assertEquals("update format not valid", cs.getMessage());
		}
	}

	@Test
	public void testUpdateServiceInstanceFailsWithoutToKey() {
		Plan fromPlan = new DefaultBlueprintPlan("planId",
				"testPlan",
				"test plan description",
				"testplanApp",
				ImmutableMap.of(
						"update",ImmutableList.of(
						ImmutableMap.of("effector",ImmutableMap.of(
								"name","resize",
								"params",ImmutableMap.of()))))
		);

		Plan toPlan = new DefaultBlueprintPlan("planId2",
				"testPlan2",
				"test plan description",
				"testplanApp",
				ImmutableMap.of(
						"update", ImmutableList.of(
						ImmutableMap.of(
								"to", "testPlan",
								"effector", ImmutableMap.of(
										"name", "resize",
										"params", ImmutableMap.of())))));

		try {
			testDefaultBlueprintPlan(fromPlan, toPlan);
			fail();
		} catch (ServiceInstanceUpdateNotSupportedException cs) {
			assertEquals("Current plan cannot be updated to plan " + toPlan.getId(), cs.getMessage());
		}
	}

	@Test
	public void testUpdateServiceInstanceFailsWithEmptyUpdate()
			throws ServiceInstanceExistsException, ServiceBrokerException {

		Plan fromPlan = new DefaultBlueprintPlan("planId",
				"testPlan",
				"test plan description",
				"testplanApp",
				ImmutableMap.of("update", ImmutableList.of())
		);

		Plan toPlan = new DefaultBlueprintPlan("planId2",
				"testPlan2",
				"test plan description",
				"testplanApp",
				ImmutableMap.of("update", ImmutableList.of(
						ImmutableMap.of(
								"to", "testPlan",
								"effector", ImmutableMap.of(
										"name", "resize",
										"params", ImmutableMap.of())))));
		try {
			testDefaultBlueprintPlan(fromPlan, toPlan);
			fail();
		} catch (ServiceInstanceUpdateNotSupportedException cs) {
			assertEquals("Current plan cannot be updated to plan " + toPlan.getId(), cs.getMessage());
		}
	}

	@Test
	public void testUpdateServiceInstanceFailsWithUpdateToNonExistentPlan()
			throws ServiceInstanceExistsException, ServiceBrokerException {

		Plan fromPlan = new DefaultBlueprintPlan("planId",
				"testPlan",
				"test plan description",
				"testplanApp",
				ImmutableMap.of(
						"update", ImmutableList.of(
						ImmutableMap.of(
								"to", "noSuchPlan",
								"effector", ImmutableMap.of(
										"name", "resize",
										"params", ImmutableMap.of())))));

		Plan toPlan = new DefaultBlueprintPlan("planId2",
				"testPlan2",
				"test plan description",
				"testplanApp",
				ImmutableMap.of(
						"update", ImmutableList.of(
						ImmutableMap.of(
								"to", "testPlan",
								"effector", ImmutableMap.of(
										"name", "resize",
										"params", ImmutableMap.of())))));
		try {
			testDefaultBlueprintPlan(fromPlan, toPlan);
			fail();
		} catch (ServiceInstanceUpdateNotSupportedException cs) {
			assertEquals("Current plan cannot be updated to plan " + toPlan.getId(), cs.getMessage());
		}
	}

	@Test(expected=ServiceInstanceUpdateNotSupportedException.class)
	public void testUpdateServiceInstanceWithOutBlueprintMetadata()
		throws ServiceInstanceUpdateNotSupportedException {

		when(repository.findOne(any(String.class))).thenReturn(testServiceInstance);
		when(catalogService.getServiceDefinition(any(String.class))).thenReturn(serviceDefinition);
		when(serviceDefinition.getMetadata()).thenReturn(ImmutableMap.of("brooklynServices", ImmutableList.of(ImmutableMap.of("type", "testType"))));
		when(serviceDefinition.getPlans()).thenReturn(ImmutableList.of(
				new DefaultBlueprintPlan("planId", "planName", "planDescription", "Test App", ImmutableMap.of("brooklynServices", ImmutableList.of(ImmutableMap.of("type", "testType"))))
		));

		UpdateServiceInstanceRequest request = new UpdateServiceInstanceRequest(serviceDefinition.getId(), "planID");

		UpdateServiceInstanceResponse response = service.updateServiceInstance(request);

	}

	@Test(expected=ServiceInstanceUpdateNotSupportedException.class)
	public void testUpdateServiceInstanceWithOutUpgradeMetadata()
			throws ServiceInstanceUpdateNotSupportedException {

		when(repository.findOne(any(String.class))).thenReturn(testServiceInstance);
		when(catalogService.getServiceDefinition(any(String.class))).thenReturn(serviceDefinition);
		when(serviceDefinition.getMetadata()).thenReturn(ImmutableMap.of("brooklynServices", ImmutableList.of(ImmutableMap.of("type", "testType"))));
		when(serviceDefinition.getPlans()).thenReturn(ImmutableList.of(
				new DefaultBlueprintPlan("planId", "planName", "planDescription", "Test App", ImmutableMap.of(
						"brooklynServices", ImmutableList.of(ImmutableMap.of("type", "testType")),
						"displayName", "Test App"))
		));

		UpdateServiceInstanceRequest request = new UpdateServiceInstanceRequest(serviceDefinition.getId(), "planID");

		UpdateServiceInstanceResponse response = service.updateServiceInstance(request);

	}

	private void testDefaultBlueprintPlan(Plan fromPlan, Plan toPlan){
		when(repository.findOne(any(String.class))).thenReturn(testServiceInstance);
		when(catalogService.getServiceDefinition(any(String.class))).thenReturn(serviceDefinition);
		when(serviceDefinition.getMetadata()).thenReturn(ImmutableMap.of("brooklynServices", ImmutableList.of(ImmutableMap.of("type", "testType"))));
		when(serviceDefinition.getPlans()).thenReturn(ImmutableList.of(fromPlan, toPlan));
		UpdateServiceInstanceRequest request = new UpdateServiceInstanceRequest(serviceDefinition.getId(), toPlan.getId());
		UpdateServiceInstanceResponse response = service.updateServiceInstance(request);
	}



}
