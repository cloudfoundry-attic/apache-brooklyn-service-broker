package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.brooklyn.rest.client.BrooklynApi;
import org.apache.brooklyn.rest.domain.CatalogItemSummary;
import org.apache.brooklyn.rest.domain.EntitySummary;
import org.apache.brooklyn.rest.domain.LocationSummary;
import org.apache.brooklyn.rest.domain.SensorSummary;
import org.apache.brooklyn.rest.domain.TaskSummary;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.http.HttpTool;
import org.apache.brooklyn.util.http.HttpToolResponse;
import org.apache.brooklyn.util.repeat.Repeater;
import org.apache.brooklyn.util.text.Strings;
import org.apache.brooklyn.util.time.Duration;
import org.apache.http.client.HttpClient;
import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.google.api.client.util.Maps;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.BaseEncoding;

@Service
public class BrooklynRestAdmin {

    private static final Logger LOG = LoggerFactory.getLogger(BrooklynRestAdmin.class);

    private static final Predicate<String> ENTITY_GLOBAL_BLACKLIST_PREDICATE = s -> !ImmutableSet.of(
            "org.apache.brooklyn.entity.group.QuarantineGroup",
            "brooklyn.networking.subnet.SubnetTier"
    ).contains(s);

    private static final Predicate<String> ENTITY_GLOBAL_WHITELIST_PREDICATE = s -> ImmutableSet.of(

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
    private BrooklynApi brooklynApi;
    private BrooklynConfig config;

    @Autowired
    public BrooklynRestAdmin(BrooklynApi restApi, HttpClient httpClient, BrooklynConfig config) {
        this.brooklynApi = restApi;
        this.httpClient = httpClient;
        this.config = config;
    }

    public void createRepositoryIfNotExists() {
        try {
            Repositories.createRepositories(getRestApi());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async
    public Future<List<CatalogItemSummary>> getCatalogApplications(boolean includeAllVersions) {
        return new AsyncResult<>(getRestApi().getCatalogApi().listApplications("", "", includeAllVersions));
    }

    @Async
    public Future<List<LocationSummary>> getLocations() {
        return new AsyncResult<>(getRestApi().getLocationApi().list());
    }

    @Async
    public Future<TaskSummary> createApplication(String applicationSpec) {
        Response response = getRestApi().getApplicationApi().createFromForm(applicationSpec);
        return new AsyncResult<>(BrooklynApi.getEntity(response, TaskSummary.class));
    }

    @Async
    public Future<TaskSummary> deleteApplication(String id) {
        Response response = getRestApi().getEntityApi().expunge(id, id, true);
        return new AsyncResult<>(BrooklynApi.getEntity(response, TaskSummary.class));
    }

    @Async
    public Future<Map<String, Object>> getApplicationSensors(String application) {
        return new AsyncResult<>(getApplicationSensors(application, getRestApi().getEntityApi().list(application), x -> true, x -> true, x -> true, x -> true));
    }

    @Async
    public Future<Map<String, Object>> getCredentialsFromSensors(String application,
            Predicate<? super String> sensorWhitelist,
            Predicate<? super String> sensorBlacklist,
            Predicate<? super String> entityWhitelist,
            Predicate<? super String> entityBlacklist) {

        List<EntitySummary> entities = getRestApi().getEntityApi().list(application);
        if (entities.size() == 0) {
            return new AsyncResult<>(getEntitySensors(application, application, sensorWhitelist, sensorBlacklist, entityWhitelist, entityBlacklist));
        } else if (entities.size() == 1) {
            String entity = entities.get(0).getId();
            return new AsyncResult<>(getEntitySensors(application, entity, sensorWhitelist, sensorBlacklist, entityWhitelist, entityBlacklist));
        }
        return new AsyncResult<>(getApplicationSensors(application, entities, sensorWhitelist, sensorBlacklist, entityWhitelist, entityBlacklist));
    }

    private Map<String, Object> getApplicationSensors(String application, List<EntitySummary> entities,
            Predicate<? super String> sensorWhitelist,
            Predicate<? super String> sensorBlacklist,
            Predicate<? super String> entityWhitelist,
            Predicate<? super String> entityBlacklist) {

        Map<String, Object> result = new HashMap<>();
        for (EntitySummary s : entities) {
            Map<String, Object> entitySensors = getEntitySensors(application, s.getId(), sensorWhitelist, sensorBlacklist, entityWhitelist, entityBlacklist);

            if (ENTITY_GLOBAL_WHITELIST_PREDICATE.or(entityWhitelist).and(ENTITY_GLOBAL_BLACKLIST_PREDICATE.and(entityBlacklist)).test(s.getType())) {
                result.put(s.getName(), entitySensors);
            } else {
                if (entitySensors.containsKey("children")) {
                    result.putAll((Map<String, Object>) entitySensors.get("children"));
                }
            }
        }
        return result;
    }

    private Map<String, Object> getEntitySensors(String application, String entity,
            Predicate<? super String> sensorWhitelist,
            Predicate<? super String> sensorBlacklist,
            Predicate<? super String> entityWhitelist,
            Predicate<? super String> entityBlacklist) {

        Map<String, Object> sensors = getSensors(application, entity, sensorWhitelist, sensorBlacklist);
        Map<String, Object> childSensors = getApplicationSensors(application, getRestApi().getEntityApi().getChildren(application, entity), sensorWhitelist, sensorBlacklist, entityWhitelist, entityBlacklist);
        if (childSensors.size() > 0) {
            sensors.put("children", childSensors);
        }
        return sensors;
    }

    private Map<String, Object> getSensors(String application, String entity, Predicate<? super String> sensorWhitelistfilter, Predicate<? super String> sensorBlacklistFilter) {
        List<SensorSummary> sensorSummaries = getRestApi().getSensorApi().list(application, entity);
        Set<String> sensorNames = sensorSummaries.stream()
                .map(SensorSummary::getName)
                .collect(Collectors.toSet());
        Map<String, Object> result = Maps.newHashMap();
        sensorNames.stream()
                .filter(sensorName -> !sensorName.startsWith("mapped."))
                .filter(SENSOR_GLOBAL_BLACKLIST_PREDICATE.and(sensorBlacklistFilter).and(SENSOR_GLOBAL_WHITELIST_PREDICATE.or(sensorWhitelistfilter)))
                .forEach(sensorName -> {
                    Object value = sensorNames.contains("mapped." + sensorName) ?
                            getRestApi().getSensorApi().get(application, entity, "mapped." + sensorName, false) :
                            getRestApi().getSensorApi().get(application, entity, sensorName, false);
                    if (value != null) {
                        result.put(sensorName, value);
                    }
                });
        return result;
    }

    @Async
    public Future<String> postBlueprint(String file) {
        Response response = getRestApi().getCatalogApi().create(file);
        return new AsyncResult<>(BrooklynApi.getEntity(response, String.class));
    }

    @Async
    public void deleteCatalogEntry(String name, String version) throws Exception {
        getRestApi().getCatalogApi().deleteEntity(name, version);
    }

    @Async
    public Future<Boolean> hasEffector(String application, String entity, String effector) {
        return new AsyncResult<>(getRestApi().getEffectorApi().list(application, entity).stream()
                .anyMatch(effectorSummary -> effectorSummary.getName().equals(effector)));
    }

    @Async
    public Future<String> invokeEffector(String application, String entity, String effector, Map<String, Object> params) {
        return invokeEffector(application, entity, effector, "", params);
    }

    @Async
    public Future<String> invokeEffector(String application, String entity, String effector, String timeout, Map<String, Object> params) {
        try {
            Response response = getRestApi().getEffectorApi().invoke(application, entity, effector, timeout, params);
            return new AsyncResult<>(BrooklynApi.getEntity(response, String.class));
        } catch (Exception e) {
            Exceptions.propagateIfFatal(e);
            LOG.info("unable to invoke effector={},  message={}", effector, e.getMessage());
            return new AsyncResult<>(null);
        }
    }

    @Async
    public Future<Map<String, Object>> getApplicationEffectors(String application) {
        Map<String, Object> result = new HashMap<>();
        result.put("children", getApplicationEffectors(application, getRestApi().getEntityApi().list(application)));
        result.put(application, getEffectors(application, application));
        return new AsyncResult<>(result);
    }

    @Async
    public Future<Map<String, Object>> getApplicationEffectors(String application, List<EntitySummary> entities) {
        return new AsyncResult<>(_getApplicationEffectors(application, entities));

    }

    private Map<String, Object> _getApplicationEffectors(String application, List<EntitySummary> entities) {
        return entities.stream().collect(Collectors.toMap(
                entitySummary -> entitySummary.getName(),
                entitySummary -> {
                    String entity = entitySummary.getId();
                    return getEffectors(application, entity).put("children", _getApplicationEffectors(application,
                            getRestApi().getEntityApi().getChildren(application, entity)));
                }
        ));
    }

    private Map<String, Object> getEffectors(String application, String entity) {
        return getRestApi().getEffectorApi().list(application, entity).stream().collect(Collectors.toMap(
                effectorSummary -> entity + ":" + effectorSummary.getName(),
                Function.identity()
        ));
    }

    @Async
    public Future<Boolean> isApplicationRunning(String application) {
        Object result = getRestApi().getSensorApi().get(application, application, "service.state", false);

        if (result instanceof String) {
            return new AsyncResult<>(result.toString().toUpperCase().equals("RUNNING"));
        }
        return new AsyncResult<>(false);
    }

    @Async
    public Future<String> getDashboardUrl(String application) {
        // search in breadth first order for first sensor that matches
        List<EntitySummary> entities = getRestApi().getEntityApi().list(application);
        Deque<EntitySummary> q = new ArrayDeque<>(entities);
        while (!q.isEmpty()) {
            EntitySummary e = q.remove();
            List<SensorSummary> sensors = getRestApi().getSensorApi().list(application, e.getId());
            for (SensorSummary sensor : sensors) {
                if (sensor.getName().equals("management.url")) {
                    String url = String.valueOf(getRestApi().getSensorApi().get(application, e.getId(), sensor.getName(), false));
                    LOG.info("found dashboard url={} for application={}", url, application);
                    return new AsyncResult<>(url);
                }
            }
            q.addAll(getRestApi().getEntityApi().getChildren(application, e.getId()));
        }

        LOG.info("no dashboard url found for application={}", application);
        return new AsyncResult<>(null);
    }

    @Async
    public Future<Map<String, Object>> getConfigAsMap(String application, String entity, String key) {
        Object object;
        try {
            object = getRestApi().getEntityConfigApi().get(application, entity, key, false);
        } catch (Exception e) {
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
        try {
            Object object = getRestApi().getSensorApi().get(application, application, "service.state", false);
            return new AsyncResult<>(object.toString());
        } catch (Exception e) {
            return new AsyncResult<>(null);
        }

    }

    @Async
    public void deleteConfig(String application, String entity, String key) {
        try {
            getRestApi().getEntityConfigApi().set(application, entity, key, false, "");
        } catch (Exception e) {
            LOG.error("unable to delete {} {}", key, e.getMessage());
        }
    }

    @Async
    public Future<Object> setConfig(String application, String entity, String key, Object value) {
        try {
            getRestApi().getEntityConfigApi().set(application, entity, key, false, value);
            return new AsyncResult<>(value);
        } catch (Exception e) {
            LOG.error("unable to save {} {}", value, e.getMessage());
            return new AsyncResult<>(null);
        }
    }

    @Async
    public Future<String> getIconAsBase64(String url) {
        if (Strings.isEmpty(url)) return new AsyncResult<>(null);
        try {
            HttpToolResponse response = HttpTool.httpGet(httpClient, new URI(config.toFullUrl(url)), Collections.<String, String>emptyMap());
            if (response.getResponseCode() / 100 != 2) {
                // url is not relative, assume it is absolute
                return new AsyncResult<>(url);
            }
            Optional<Entry<String, List<String>>> entry = response.getHeaderLists().entrySet()
                    .stream()
                    .filter(e -> e.getKey().toLowerCase().equals("content-type"))
                    .findFirst();
            if (entry.isPresent() && !entry.get().getValue().get(0).startsWith("image")) {
                LOG.error("expected content type to start 'image' but found: {}", entry.get().getValue().get(0));
                return new AsyncResult<>(null);
            }
            return new AsyncResult<>("data:img/png;base64," + BaseEncoding.base64().encode(response.getContent()));
        } catch (Exception e) {
            LOG.error("unable to encode icon as base64");
            return new AsyncResult<>(url);
        }
    }

    public void blockUntilTaskCompletes(String id) throws PollingException {
        blockUntilTaskCompletes(id, Duration.PRACTICALLY_FOREVER);
    }

    public void blockUntilTaskCompletes(String id, Duration timeout) throws PollingException {
        try {
            Repeater.create()
                    .every(Duration.ONE_SECOND)
                    .until(() -> {
                        TaskSummary summary = getRestApi().getActivityApi().get(id);
                        if (summary.isError() || summary.isCancelled() || (summary.getSubmitTimeUtc() == null)) {
                            throw new PollingException(new IllegalStateException("Effector call failed: " + summary));
                        }
                        return summary.getEndTimeUtc() != null;
                    })
                    .rethrowExceptionImmediately()
                    .limitTimeTo(timeout)
                    .runRequiringTrue();
        } catch (Exception e) {
            LOG.error("Failed to get task summary: " + e);
            throw new PollingException(e);
        }
    }

    @VisibleForTesting
    public BrooklynApi getRestApi() {
        return brooklynApi;
    }

}
