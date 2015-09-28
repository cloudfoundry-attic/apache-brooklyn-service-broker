package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.brooklyn.rest.client.BrooklynApi;
import org.apache.brooklyn.rest.domain.CatalogItemSummary;
import org.apache.brooklyn.rest.domain.EffectorSummary;
import org.apache.brooklyn.rest.domain.EntitySummary;
import org.apache.brooklyn.rest.domain.LocationSummary;
import org.apache.brooklyn.rest.domain.SensorSummary;
import org.apache.brooklyn.rest.domain.TaskSummary;
import org.apache.brooklyn.util.core.http.HttpTool;
import org.apache.brooklyn.util.core.http.HttpToolResponse;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.text.Strings;
import org.apache.http.client.HttpClient;
import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.BaseEncoding;

@Service
public class BrooklynRestAdmin {

	private static final Logger LOG = LoggerFactory.getLogger(BrooklynRestAdmin.class);
	
	private static final Predicate<String> ENTITY_GLOBAL_BLACKLIST_PREDICATE = s -> !ImmutableSet.of(
			"org.apache.brooklyn.entity.group.QuarantineGroup",
			"brooklyn.networking.subnet.SubnetTier"
	).contains(s);

	private static final Predicate<String> SENSOR_GLOBAL_BLACKLIST_PREDICATE = s -> !ImmutableSet.of(
            "download.url",
            "expandedinstall.dir",
            "install.dir",
            "download.url.debian",
            "download.url.mac",
            "download.url.rhelcentos",
            "download.url.ubuntu"
    ).contains(s);

    private static final Predicate<String> SENSOR_GLOBAL_WHITELIST_PREDICATE = s -> ImmutableSet.of(
            "host.name",
            "host.address",
            "host.sshAddress"
    ).contains(s);

	private HttpClient httpClient;
	private BrooklynApi restApi;
	private BrooklynConfig config;

    @Autowired
    public BrooklynRestAdmin(BrooklynApi restApi, HttpClient httpClient, BrooklynConfig config) {
        this.restApi = restApi;
		this.httpClient = httpClient;
		this.config = config;
    }

	public void createRepositoryIfNotExists(){
		try{
			Repositories.createRepositories(restApi);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

    @Async
	public Future<List<CatalogItemSummary>> getCatalogApplications(boolean includeAllVersions){
		return new AsyncResult<>(restApi.getCatalogApi().listApplications("", "", includeAllVersions));
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
        return new AsyncResult<>(getApplicationSensors(application, restApi.getEntityApi().list(application), Predicates.alwaysTrue(), Predicates.alwaysTrue()));
	}

    @Async
    public Future<Map<String, Object>> getCredentialsFromSensors(String application, Predicate<? super String> sensorFilter, Predicate<? super String> entityFilter) {
        List<EntitySummary> entities = restApi.getEntityApi().list(application);
        if (entities.size() == 0) {
            return new AsyncResult<>(getEntitySensors(application, application, sensorFilter, entityFilter));
        } else if (entities.size() == 1) {
            String entity = entities.get(0).getId();
            return new AsyncResult<>(getEntitySensors(application, entity, sensorFilter, entityFilter));
        }
        return new AsyncResult<>(getApplicationSensors(application, entities, sensorFilter, entityFilter));
    }
	
	private Map<String, Object> getApplicationSensors(String application, List<EntitySummary> entities, Predicate<? super String> sensorFilter, Predicate<? super String> entityFilter){
		Map<String, Object> result = new HashMap<>();
		for (EntitySummary s : entities) {
			Map<String, Object> entitySensors = getEntitySensors(application, s.getId(), sensorFilter, entityFilter);
			
			if(Predicates.and(entityFilter, ENTITY_GLOBAL_BLACKLIST_PREDICATE).apply(s.getType())){
				result.put(s.getName(), entitySensors);
			} else {
				if(entitySensors.containsKey("children")) {
					result.putAll((Map<String, Object>)entitySensors.get("children"));
				}
			}
		}
		return result;
	}

	private Map<String, Object> getEntitySensors(String application, String entity, Predicate<? super String> sensorFilter, Predicate<? super String> entityFilter) {
		Map<String, Object> sensors = getSensors(application, entity, sensorFilter);
		Map<String, Object> childSensors = getApplicationSensors(application, restApi.getEntityApi().getChildren(application, entity), sensorFilter, entityFilter);
		if(childSensors.size() > 0){
		  sensors.put("children", childSensors);
		}
		return sensors;
	}
	
	private Map<String, Object> getSensors(String application, String entity, Predicate<? super String> filter){
		Map<String, Object> sensors = new HashMap<>();

        List<SensorSummary> sensorSummaries = restApi.getSensorApi().list(application, entity);
        Set<String> sensorNames = sensorSummaries.stream().map(SensorSummary::getName).collect(Collectors.toSet());

		for (String sensorName : sensorNames) {
			if (sensorName.startsWith("mapped.")) {
                continue;
            }
            if (Predicates.and(SENSOR_GLOBAL_BLACKLIST_PREDICATE, Predicates.or(SENSOR_GLOBAL_WHITELIST_PREDICATE, filter)).apply(sensorName)) {
            	LOG.info("Using sensor={} while making credentials", sensorName);
                Object value = sensorNames.contains("mapped." + sensorName) ?
                    restApi.getSensorApi().get(application, entity, "mapped." + sensorName, false) :
                        restApi.getSensorApi().get(application, entity, sensorName, false);
                sensors.put(sensorName, value);
            } else {
            	LOG.info("Ignoring sensorName={} while making credentials", sensorName);
            }
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
	public Future<String> invokeEffector(String application, String entity, String effector, String timeout, Map<String, Object> params){
    	try{
    		Response response = restApi.getEffectorApi().invoke(application, entity, effector, timeout, params);
    		return new AsyncResult<>(BrooklynApi.getEntity(response, String.class));
    	} catch (Exception e) {
    		Exceptions.propagateIfFatal(e);
    		LOG.info("unable to invoke effector={},  message={}", effector, e.getMessage());
    		return new AsyncResult<>(null);
    	}
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
        Object result = restApi.getSensorApi().get(application, application, "service.state", false);

		if (result instanceof String){
			return new AsyncResult<>(result.toString().toUpperCase().equals("RUNNING"));
		}
		return new AsyncResult<>(false);
	}

    @Async
	public Future<String> getDashboardUrl(String application) {
    	// search in breadth first order for first sensor that matches
    	List<EntitySummary> entities = restApi.getEntityApi().list(application);
		Deque<EntitySummary> q = new ArrayDeque<>(entities);
		while(!q.isEmpty()) {
			EntitySummary e = q.remove();
			List<SensorSummary> sensors = restApi.getSensorApi().list(application, e.getId());
			for(SensorSummary sensor : sensors) {
				if(sensor.getName().equals("management.url")){
				  String url = String.valueOf(restApi.getSensorApi().get(application, e.getId(), sensor.getName(), false));
				  LOG.info("found dashboard url={} for application={}", url, application);
				  return new AsyncResult<>(url);
				}
			}
			q.addAll(restApi.getEntityApi().getChildren(application, e.getId()));
		}
		
		LOG.info("no dashboard url found for application={}", application);
		return new AsyncResult<>(null);
	}
    
    @Async
    public Future<Map<String, Object>> getConfigAsMap(String application, String entity, String key){
    	Object object;
		try{
			object = restApi.getEntityConfigApi().get(application, entity, key, false);
		}catch(Exception e){
			LOG.error("Unable to get config with key={}", key);
			return new AsyncResult<>(null);
		}
		
		if (object == null || !(object instanceof Map)) {
			LOG.error("Unable to get Map with key={}", key);
			return new AsyncResult<>(null);
		}
		Map<String, Object> map = (Map<String, Object>) object;
		return new AsyncResult<>(map);
    }

    @Async
	public Future<String> getServiceState(String application) {
    	try{
    		Object object = restApi.getSensorApi().get(application, application, "service.state", false);
    		return new AsyncResult<>(object.toString());
    	} catch (Exception e) {
    		return new AsyncResult<>(null);
    	}
        
	}

	@Async
	public void deleteConfig(String application, String entity, String key) {
		try{
			restApi.getEntityConfigApi().set(application, entity, key, false, "");
		} catch(Exception e){
			LOG.error("unable to delete {} {}", key, e.getMessage());
		}
	}
	
	@Async
	public Future<Object> setConfig(String application, String entity, String key, Object value) {
		try{
			restApi.getEntityConfigApi().set(application, entity, key, false, value);
			return new AsyncResult<>(value);
		} catch(Exception e){
			LOG.error("unable to save {} {}", value, e.getMessage());
			return new AsyncResult<>(null);
		}
	}

	@Async
	public Future<String> getIconAsBase64(String url){
		if (Strings.isEmpty(url)) return new AsyncResult<>(null);
		try {
			HttpToolResponse response = HttpTool.httpGet(httpClient, new URI(config.toFullUrl(url)), Collections.<String, String>emptyMap());
			return new AsyncResult<>("data:img/png;base64," + BaseEncoding.base64().encode(response.getContent()));
		} catch (Exception e) {
			LOG.error("unable to encode icon as base64");
			return new AsyncResult<>(null);
		}
	}
}
