package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import brooklyn.rest.client.BrooklynApi;
import brooklyn.rest.domain.CatalogItemSummary;
import brooklyn.rest.domain.EffectorSummary;
import brooklyn.rest.domain.EntitySummary;
import brooklyn.rest.domain.LocationSummary;
import brooklyn.rest.domain.TaskSummary;

@Service
public class BrooklynRestAdmin {
	

	@Autowired
	private BrooklynApi restApi;
	
	private Set<String> sensorBlacklist = new HashSet<String>(Arrays.asList(
			"download.url",
			"expandedinstall.dir",
			"install.dir"
	));

	
	public List<CatalogItemSummary> getCatalogApplicaitons(){
		return restApi.getCatalogApi().listApplications("", "");
	}

	public List<LocationSummary> getLocations() {
		return restApi.getLocationApi().list();
	}
	
	public TaskSummary createApplication(String applicationSpec){
		Response response = restApi.getApplicationApi().createFromForm(applicationSpec);
		return BrooklynApi.getEntity(response, TaskSummary.class);
	}
	
	public TaskSummary deleteApplication(String id) {
		System.out.println("deleting id " + id);
		Response response = restApi.getEntityApi().expunge(id, id, true);
		//Response response = restApi.getApplicationApi().delete(id);
		return BrooklynApi.getEntity(response, TaskSummary.class);	
	}
	
	public Map<String, Object> getApplicationSensors(String application){
		return getApplicationSensors(application, restApi.getEntityApi().list(application));
	}
	
	private Map<String, Object> getApplicationSensors(String application, List<EntitySummary> entities){
		Map<String, Object> result = new HashMap<String, Object>();
		for (EntitySummary s : entities) {
			String entity = s.getId();
			Map<String, Object> sensors = getSensors(application, entity);
			Map<String, Object> childSensors = getApplicationSensors(
					application,
					restApi.getEntityApi().getChildren(application, entity));
			sensors.put("children", childSensors);
			result.put(s.getName(), sensors);
		}
		return result;
	}
	
	private Map<String, Object> getSensors(String application, String entity){
		Map<String, Object> sensors = new HashMap<String, Object>();
		for (brooklyn.rest.domain.SensorSummary sensorSummary : restApi.getSensorApi().list(application, entity)) {
			String sensor = sensorSummary.getName();
			if(sensorBlacklist.contains(sensor)) continue;
			sensors.put(sensor, restApi.getSensorApi().get(application, entity, sensor, false));
		}	
		return sensors;
	}

	public String postBlueprint(String file) {
		Response response = restApi.getCatalogApi().create(file);
		return BrooklynApi.getEntity(response, String.class);
	}

	public void deleteCatalogEntry(String name, String version) throws Exception {
		restApi.getCatalogApi().deleteEntity(name, version);
	}
	
	public String invokeEffector(String application, String entity, String effector, Map<String, Object> params){
		// TODO Complete these params
		Response response = restApi.getEffectorApi().invoke(application, entity, effector, "", params);
		return BrooklynApi.getEntity(response, String.class);
	}
	
	public Map<String, Object> getApplicationEffectors(String application){
		Map<String, Object> result = new HashMap<String, Object>();
		Map<String, Object> effectors = getEffectors(application, application);
		result.put("children",  getApplicationEffectors(application, restApi.getEntityApi().list(application)));
		result.put(application, effectors);
		return result;
	}

	public Map<String, Object> getApplicationEffectors(String application, List<EntitySummary> entities) {
		Map<String, Object> result = new HashMap<String, Object>();
		for (EntitySummary s : entities) {
			String entity = s.getId();
			Map<String, Object> effectors = getEffectors(application, entity);
			Map<String, Object> childEntities = getApplicationEffectors(
					application,
					restApi.getEntityApi().getChildren(application, entity));
			effectors.put("children", childEntities);
			result.put(s.getName(), effectors);
		}
		return result;
		
	}

	private Map<String, Object> getEffectors(String application, String entity) {
		Map<String, Object> effectors = new HashMap<String, Object>();
		for (EffectorSummary effectorSummary : restApi.getEffectorApi().list(application, entity)) {
			effectors.put(entity +":"+ effectorSummary.getName(), effectorSummary);
		}	
		return effectors;
	}

	public Boolean isApplicationRunning(String application) {
		Object result = restApi.getSensorApi().get(application, application, "service.isUp", false);
		if (result instanceof Boolean){
			return (Boolean) result;
		}
		return false;
	}

}
