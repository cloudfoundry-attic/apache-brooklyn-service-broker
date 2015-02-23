package org.cloudfoundry.community.servicebroker.brooklyn.controller;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceRepository;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import brooklyn.util.stream.Streams;

@RestController
public class BrooklynController {
	
	private BrooklynRestAdmin admin;
	private BrooklynServiceInstanceRepository instanceRepository;

	@Autowired
	public BrooklynController(BrooklynRestAdmin admin, BrooklynServiceInstanceRepository instanceRepository) {
		this.admin = admin;
		this.instanceRepository = instanceRepository;
	}
	
	@RequestMapping(value = "/create", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED)
	public void create(InputStream uploadedInputStream){
		admin.postBlueprint(Streams.readFullyString(uploadedInputStream));
		// TODO create a response
	}
	
	@RequestMapping(value = "/delete/{name}/{version:.+}/", method = RequestMethod.DELETE)
	public void delete(@PathVariable("name") String name, @PathVariable("version") String version){
		try {
			admin.deleteCatalogEntry(name, version);
		} catch (Exception e) {
			// TODO create a response	
		}
	}
	
	@RequestMapping(value = "/invoke/{application}/{entity}/{effector}", method = RequestMethod.POST,  consumes = MediaType.APPLICATION_JSON)
	public Object invoke(
			@PathVariable("application") String application, 
			@PathVariable("entity") String entity, 
			@PathVariable("effector") String effector,
			@RequestBody Map<String, Object> params) {
		
		ServiceInstance instance = instanceRepository.findOne(application);
		if (instance != null) {
			String appId = instance.getServiceDefinitionId();
			return admin.invokeEffector(appId, entity, effector, params);
		}
		
		return new Object();
	}
	
	@RequestMapping(value = "/effectors/{application}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> effectors(@PathVariable("application") String application){
		ServiceInstance instance = instanceRepository.findOne(application);
		if (instance != null) {
			String appId = instance.getServiceDefinitionId();
			return admin.getApplicationEffectors(appId);
		}	
		System.out.println(instanceRepository);
		return Collections.emptyMap();
	}
	
	@RequestMapping(value = "/sensors/{application}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> sensors(@PathVariable("application") String application) {
		ServiceInstance instance = instanceRepository.findOne(application);
		if (instance != null) {
			String appId = instance.getServiceDefinitionId();
			return admin.getApplicationSensors(appId);
		}	
		System.out.println(instanceRepository);
		return Collections.emptyMap();
	}
	
	@RequestMapping(value = "/is-running/{application}")
	public @ResponseBody Boolean isRunning(@PathVariable("application") String application) {
		ServiceInstance instance = instanceRepository.findOne(application);
		if (instance != null) {
			String appId = instance.getServiceDefinitionId();
			return admin.isApplicationRunning(appId);
		}
		
		return false;
	}
}
