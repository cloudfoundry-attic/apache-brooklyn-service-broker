package org.cloudfoundry.community.servicebroker.brooklyn.service;

import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceRepository;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.Respositories;
import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceExistsException;
import org.cloudfoundry.community.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import brooklyn.rest.domain.TaskSummary;

@Service
public class BrooklynServiceInstanceService implements ServiceInstanceService {


	private BrooklynRestAdmin admin;
	private BrooklynServiceInstanceRepository repository;

	@Autowired
	public BrooklynServiceInstanceService(BrooklynRestAdmin admin, BrooklynServiceInstanceRepository repository) {
		this.admin = admin;
		this.repository = repository;
	}

	@Override
	public ServiceInstance createServiceInstance(ServiceDefinition service,
			String serviceInstanceId, String planId, String organizationGuid,
			String spaceGuid) throws ServiceInstanceExistsException,
			ServiceBrokerException {
		
		admin.createRepositoryIfNotExists();

		// check if exists already
		ServiceInstance instance = getServiceInstance(serviceInstanceId);
		if (instance != null) {
			throw new ServiceInstanceExistsException(instance);
		}
		System.out.println("-- creating service --");
		System.out.println(serviceInstanceId);
		System.out.println(planId);
		System.out.println(organizationGuid);
		System.out.println(spaceGuid);
		System.out.println("----------------------");

		String location = "localhost"; // default
		for(Plan p : service.getPlans()){
			if(p.getId().equals(planId)){
				location = p.getName();
			}
		}
		
		TaskSummary taskSummary = admin.createApplication(
				"{\"services\":[\"type\": \"" + service.getId() + "\"], "
				+ "\"locations\": [ \"" + location +"\"]"
				+ "}");


		instance = new ServiceInstance(serviceInstanceId,
				taskSummary.getEntityId(),
				planId, organizationGuid, spaceGuid,
				null);
		
		repository.save(instance);
		return instance;
	}

	@Override
	public ServiceInstance getServiceInstance(String id) {
		return repository.findOne(id);
	}

	@Override
	public ServiceInstance updateServiceInstance(String instanceId,
			String planId) throws ServiceInstanceUpdateNotSupportedException,
			ServiceBrokerException, ServiceInstanceDoesNotExistException {
		throw new ServiceInstanceUpdateNotSupportedException("");
	}

	@Override
	public ServiceInstance deleteServiceInstance(String id, String serviceId,
			String planId) throws ServiceBrokerException {
		ServiceInstance instance = getServiceInstance(id);
		if (instance != null) {
			repository.delete(id);
			admin.deleteApplication(instance.getServiceDefinitionId());
		}
		return instance;
	}

}
