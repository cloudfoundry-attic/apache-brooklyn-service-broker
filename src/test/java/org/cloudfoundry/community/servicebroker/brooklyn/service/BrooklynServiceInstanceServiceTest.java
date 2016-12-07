package org.cloudfoundry.community.servicebroker.brooklyn.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.brooklyn.rest.domain.TaskSummary;
import org.apache.brooklyn.util.collections.MutableList;
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
	private final static BrooklynServiceInstance TEST_SERVICE_INSTANCE = new BrooklynServiceInstance(SVC_INST_ID, SVC_DEFINITION_ID).withPlanId(SVC_PLAN_ID);


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
		when(repository.findOne(any(String.class))).thenReturn(TEST_SERVICE_INSTANCE);
		CreateServiceInstanceRequest request = new CreateServiceInstanceRequest(serviceDefinition.getId(), "planId", "organizationGuid", "spaceGuid");
		service.createServiceInstance(request.withServiceInstanceId(SVC_INST_ID));
	}
	
	@Test
	public void serviceInstanceRetrievedSuccessfully() 
			throws ServiceInstanceExistsException, ServiceBrokerException{
		
		when(repository.findOne(any(String.class))).thenReturn(TEST_SERVICE_INSTANCE);
		String serviceInstanceId = TEST_SERVICE_INSTANCE.getServiceInstanceId();
		assertEquals(serviceInstanceId, service.getServiceInstance(serviceInstanceId).getServiceInstanceId());
	}
	
	@Test
	public void serviceInstanceDeletedSuccessfully() 
			throws ServiceInstanceExistsException, ServiceBrokerException {

		when(repository.findOne(any(String.class))).thenReturn(TEST_SERVICE_INSTANCE);
		String instanceId = TEST_SERVICE_INSTANCE.getServiceInstanceId();
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
        String expectedBlueprint = String.format("{\"name\":\"Test App (CFService)\",\"services\":[{\"type\": \"%s\"}], \"locations\": [\"%s\"], \"brooklyn.config\":{\"provisioning.properties\":{\"minCores\":%d,\"minRam\":%d}}}", serviceDefinition.getId(), "testLocation", TEST_MIN_CORES, TEST_MIN_RAM);
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
        
        String expectedBlueprint = String.format("{\"name\":\"Test App (CFService)\",\"services\":[{\"type\": \"%s\"}], \"locations\": [\"%s\"], \"brooklyn.config\":{\"provisioning.properties\":{\"minCores\":%d,\"minRam\":%d}}}", serviceDefinition.getId(), "testLocation", TEST_MIN_CORES, TEST_MIN_RAM);
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
        String expectedBlueprint = String.format("{\"name\":\"Test App (CFService)\",\"services\":[{\"type\": \"%s\"}], \"locations\": [\"%s\"]}", serviceDefinition.getId(), "testLocation");
        String blueprint = service.createBlueprint(serviceDefinition, request);
        // Remove whitespace for assertion so we're not tied to the implementation's whitespace rules
        assertEquals(expectedBlueprint.replace(" ", ""), blueprint.replace(" ", ""));
    }

    @Test
	public void testUpdateServiceInstance()
		throws ServiceInstanceExistsException, ServiceBrokerException {
			when(repository.findOne(any(String.class))).thenReturn(TEST_SERVICE_INSTANCE);
			when(catalogService.getServiceDefinition(any(String.class))).thenReturn(serviceDefinition);
			when(serviceDefinition.getPlans())
					.thenReturn(ImmutableList.of(
							new DefaultBlueprintPlan("planId",
									"testPlan",
									"test plan description",
									"testplanApp",
									ImmutableMap.of("update",ImmutableList.of(ImmutableMap.of("to","testPlan2","effector",ImmutableMap.of("name","resize", "params",ImmutableMap.of()))))

							),
							new DefaultBlueprintPlan("planId2",
									"testPlan2",
									"test plan description2",
									"testplanApp2",
									ImmutableMap.of("update",ImmutableList.of(ImmutableMap.of("to","testPlan","effector",ImmutableMap.of("name","resize", "params",ImmutableMap.of()))))
							)));

			String instanceId = TEST_SERVICE_INSTANCE.getServiceInstanceId();
			UpdateServiceInstanceRequest request = new UpdateServiceInstanceRequest(serviceDefinition.getId(), "planId2");

			UpdateServiceInstanceResponse response = service.updateServiceInstance(request);
			assertNotNull(response);

	}

	@Test
	public void testUpdateServiceInstanceFailedGracefully()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		List<Plan> plans= ImmutableList.of(
				new DefaultBlueprintPlan("planId2",
						"testPlan2",
						"test plan description",
						"testplanApp",
						ImmutableMap.of("update",ImmutableMap.of("to","testPlan","effector",ImmutableMap.of("name","resize", "params",ImmutableMap.of())))

				),
				new DefaultBlueprintPlan("planId2",
						"testPlan2",
						"test plan description3",
						"testplanApp3",
						ImmutableMap.of("update",ImmutableList.of(ImmutableMap.of("to","testPlan","effector",ImmutableMap.of("params",ImmutableMap.of()))))
				),
				new DefaultBlueprintPlan("planId2",
						"testPlan2",
						"test plan description4",
						"testplanApp4",
						ImmutableMap.of("update",ImmutableList.of(ImmutableMap.of("effector",ImmutableMap.of("name","resize", "params",ImmutableMap.of()))))
				));
		for(Plan plan : plans) {
			try {
				testDefaultBluePrintPlan(ImmutableList.of(plan),plan.getId());
				fail();
			} catch (ServiceInstanceUpdateNotSupportedException cs) {
				assertEquals("update format not valid", cs.getMessage());
			}
		}

	}

	@Test
	public void testUpdateServiceInstanceFailedWithEmptyUpdate()
			throws ServiceInstanceExistsException, ServiceBrokerException {
		List<Plan> plans= ImmutableList.of(
				new DefaultBlueprintPlan("planId2",
						"testPlan2",
						"test plan description2",
						"testplanApp2",
						ImmutableMap.of("update",ImmutableList.of())
				));
		for(Plan plan : plans) {
			try {
				testDefaultBluePrintPlan(ImmutableList.of(plan),plan.getId());
				fail();
			} catch (ServiceInstanceUpdateNotSupportedException cs) {
				assertEquals("Update not supported at this time", cs.getMessage());
			}
		}

	}

	@Test(expected=ServiceInstanceUpdateNotSupportedException.class)
	public void testUpdateServiceInstanceWithOutBlueprintMetaData()
		throws ServiceInstanceUpdateNotSupportedException {

		when(catalogService.getServiceDefinition(any(String.class))).thenReturn(serviceDefinition);
		when(serviceDefinition.getPlans()).thenReturn(ImmutableList.of(
				new DefaultBlueprintPlan("planId", "planName", "planDescription", "Test App", null)
		));

		UpdateServiceInstanceRequest request = new UpdateServiceInstanceRequest(serviceDefinition.getId(), "planID");

		UpdateServiceInstanceResponse response = service.updateServiceInstance(request);

	}

	@Test(expected=ServiceInstanceUpdateNotSupportedException.class)
	public void testUpdateServiceInstanceWithOutUpgradeMetaData()
			throws ServiceInstanceUpdateNotSupportedException {

		when(catalogService.getServiceDefinition(any(String.class))).thenReturn(serviceDefinition);
		when(serviceDefinition.getPlans()).thenReturn(ImmutableList.of(
				new DefaultBlueprintPlan("planId", "planName", "planDescription", "Test App", ImmutableMap.of("displayName", "hari"))
		));

		UpdateServiceInstanceRequest request = new UpdateServiceInstanceRequest(serviceDefinition.getId(), "planID");

		UpdateServiceInstanceResponse response = service.updateServiceInstance(request);

	}

	private void testDefaultBluePrintPlan(List<Plan> plans, String planId){
		when(repository.findOne(any(String.class))).thenReturn(TEST_SERVICE_INSTANCE);
		when(catalogService.getServiceDefinition(any(String.class))).thenReturn(serviceDefinition);
		when(serviceDefinition.getPlans())
				.thenReturn(MutableList.<Plan>builder().add(new DefaultBlueprintPlan("planId",
						"testPlan",
						"test plan description",
						"testplanApp",
						ImmutableMap.of("update",ImmutableList.of(ImmutableMap.of("to","testPlan2","effector",ImmutableMap.of("name","resize", "params",ImmutableMap.of()))))

				)).addAll(plans).build());
		UpdateServiceInstanceRequest request = new UpdateServiceInstanceRequest(serviceDefinition.getId(), planId);
		UpdateServiceInstanceResponse response = service.updateServiceInstance(request);


	}



}
