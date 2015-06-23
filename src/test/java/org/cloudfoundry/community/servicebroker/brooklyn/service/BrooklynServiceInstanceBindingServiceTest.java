package org.cloudfoundry.community.servicebroker.brooklyn.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.cloudfoundry.community.servicebroker.brooklyn.BrooklynConfiguration;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceBindingRepository;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.model.fixture.ServiceInstanceBindingFixture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {BrooklynConfiguration.class})
public class BrooklynServiceInstanceBindingServiceTest {

private final static String SVC_INST_BIND_ID = "serviceInstanceBindingId";
	
	@Mock
	private BrooklynRestAdmin admin;
	@Mock
	private ServiceInstance serviceInstance;
	
	private BrooklynServiceInstanceBindingService bindingService;
	
	@Mock
	private BrooklynServiceInstanceBindingRepository repository;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		bindingService = new BrooklynServiceInstanceBindingService(admin, repository);
	}
	
	@Test
	public void newServiceInstanceBindingCreatedSuccessfully() 
			throws ServiceBrokerException, ServiceInstanceBindingExistsException {

		when(admin.getApplicationSensors(any(String.class))).thenReturn(Collections.emptyMap());
		CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest(serviceInstance.getServiceDefinitionId(), "planId", "appGuid");
		ServiceInstanceBinding binding = bindingService.createServiceInstanceBinding(request.withBindingId(SVC_INST_BIND_ID));
		
		assertNotNull(binding);
		assertEquals(SVC_INST_BIND_ID, binding.getId());
	}
	
	@Test(expected=ServiceInstanceBindingExistsException.class)
	public void serviceInstanceCreationFailsWithExistingInstance()  
			throws ServiceBrokerException, ServiceInstanceBindingExistsException {
		
		when(repository.findOne(any(String.class)))
		.thenReturn(ServiceInstanceBindingFixture.getServiceInstanceBinding());	
		when(admin.getApplicationSensors(any(String.class))).thenReturn(Collections.emptyMap());
		CreateServiceInstanceBindingRequest request = new CreateServiceInstanceBindingRequest(serviceInstance.getServiceDefinitionId(), "planId", "appGuid");
				
		bindingService.createServiceInstanceBinding(request.withBindingId(SVC_INST_BIND_ID));
		bindingService.createServiceInstanceBinding(request.withBindingId(SVC_INST_BIND_ID));
	}
	
	@Test
	public void serviceInstanceBindingRetrievedSuccessfully() 
			throws ServiceBrokerException, ServiceInstanceBindingExistsException{

		ServiceInstanceBinding binding = ServiceInstanceBindingFixture.getServiceInstanceBinding();
		when(repository.findOne(any(String.class))).thenReturn(binding);
		
		assertEquals(binding.getId(), bindingService.getServiceInstanceBinding(binding.getId()).getId());
	}
	
	@Test
	public void serviceInstanceBindingDeletedSuccessfully() 
			throws ServiceBrokerException, ServiceInstanceBindingExistsException {
		
		ServiceInstanceBinding binding = ServiceInstanceBindingFixture.getServiceInstanceBinding();
		when(repository.findOne(any(String.class))).thenReturn(binding);

		DeleteServiceInstanceBindingRequest request = new DeleteServiceInstanceBindingRequest(binding.getId(), serviceInstance, "serviceId", "planId");
		assertNotNull(bindingService.deleteServiceInstanceBinding(request));
	}
}
