package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.ws.rs.core.Response;

import org.cloudfoundry.community.servicebroker.brooklyn.repository.Repositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
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
	
	private Set<String> sensorBlacklist = new HashSet<>(Arrays.asList(
			"download.url",
			"expandedinstall.dir",
			"install.dir",
			"download.url.debian",
			"download.url.mac",
			"download.url.rhelcentos",
			"download.url.ubuntu"
	));

	public void createRepositoryIfNotExists(){
		try{
			Repositories.createRepositories(restApi);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

    @Async
	public Future<List<CatalogItemSummary>> getCatalogApplications(){
		return new AsyncResult<>(restApi.getCatalogApi().listApplications("", "", false));
	}

    @Async
	public Future<List<LocationSummary>> getLocations() {
	    return new AsyncResult<>(restApi.getLocationApi().list());
	}

    @Async
	public Future<TaskSummary> createApplication(String applicationSpec){
		Response response = restApi.getApplicationApi().createFromForm(applicationSpec);
        return new AsyncResult<>(BrooklynApi.getEntity(response, TaskSummary.class));
	}

    @Async
	public Future<TaskSummary> deleteApplication(String id) {
		Response response = restApi.getEntityApi().expunge(id, id, true);
		return new AsyncResult<>(BrooklynApi.getEntity(response, TaskSummary.class));
	}

    @Async
	public Future<Map<String, Object>> getApplicationSensors(String application){
		return new AsyncResult<>(getApplicationSensors(application, restApi.getEntityApi().list(application)));
	}
	
	private Map<String, Object> getApplicationSensors(String application, List<EntitySummary> entities){
		Map<String, Object> result = new HashMap<>();
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
		Map<String, Object> sensors = new HashMap<>();
		for (brooklyn.rest.domain.SensorSummary sensorSummary : restApi.getSensorApi().list(application, entity)) {
			String sensor = sensorSummary.getName();
			if(sensorBlacklist.contains(sensor)) continue;
			sensors.put(sensor, restApi.getSensorApi().get(application, entity, sensor, false));
		}	
		return sensors;
	}

    @Async
	public Future<String> postBlueprint(String file) {
		Response response = restApi.getCatalogApi().create(file);
		return new AsyncResult<>(BrooklynApi.getEntity(response, String.class));
	}

    @Async
	public void deleteCatalogEntry(String name, String version) throws Exception {
		restApi.getCatalogApi().deleteEntity(name, version);
	}

    @Async
	public Future<String> invokeEffector(String application, String entity, String effector, Map<String, Object> params){
		// TODO Complete these params
		Response response = restApi.getEffectorApi().invoke(application, entity, effector, "", params);
		return new AsyncResult<>(BrooklynApi.getEntity(response, String.class));
	}

    @Async
	public Future<Map<String, Object>> getApplicationEffectors(String application){
		Map<String, Object> result = new HashMap<>();
		Map<String, Object> effectors = getEffectors(application, application);
		result.put("children",  getApplicationEffectors(application, restApi.getEntityApi().list(application)));
		result.put(application, effectors);
		return new AsyncResult<>(result);
	}

    @Async
	public Future<Map<String, Object>> getApplicationEffectors(String application, List<EntitySummary> entities) {
        Map<String, Object> result = _getApplicationEffectors(application, entities);
		return new AsyncResult<>(result);
		
	}

    private Map<String, Object> _getApplicationEffectors(String application, List<EntitySummary> entities) {
        Map<String, Object> result = new HashMap<>();
        for (EntitySummary s : entities) {
            String entity = s.getId();
            Map<String, Object> effectors = getEffectors(application, entity);
            Map<String, Object> childEntities = _getApplicationEffectors(
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

    @Async
	public Future<Boolean> isApplicationRunning(String application) {
		Object result = restApi.getSensorApi().get(application, application, "service.isUp", false);
		if (result instanceof Boolean){
			return new AsyncResult<>((Boolean) result);
		}
		return new AsyncResult<>(false);
	}

}
