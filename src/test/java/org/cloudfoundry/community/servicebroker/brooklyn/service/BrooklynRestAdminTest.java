package org.cloudfoundry.community.servicebroker.brooklyn.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.brooklyn.rest.api.EntityApi;
import org.apache.brooklyn.rest.api.SensorApi;
import org.apache.brooklyn.rest.client.BrooklynApi;
import org.apache.brooklyn.rest.domain.EntitySummary;
import org.apache.brooklyn.rest.domain.SensorSummary;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class BrooklynRestAdminTest {

    private static final List<EntitySummary> TEST_ENTITY_SUMMARIES = ImmutableList.of(
            new EntitySummary("test_id", "name", "test_type", "test_catalog_item_id", ImmutableMap.of())
    );
    
    private static final List<EntitySummary> TEST_CHILD_ENTITY_SUMMARIES = ImmutableList.of(
    		new EntitySummary("test_child_id", "child", "test_type", "test_catalog_item_id", ImmutableMap.of()),
    		new EntitySummary("test_child_id_2", "child2", "test_type_2", "test_catalog_item_id", ImmutableMap.of())
    );

    private static final List<SensorSummary> TEST_SENSOR_SUMMARIES = ImmutableList.of(
            new SensorSummary("sensor.one.name", "sensor.one.type", "sensor.one.description", ImmutableMap.of()),
            new SensorSummary("sensor.two.name", "sensor.two.type", "sensor.two.description", ImmutableMap.of()),
            new SensorSummary("host.name", "sensor.two.type", "myHostName", ImmutableMap.of())
    );

    private static final List<String> SENSOR_WHITELIST = ImmutableList.of("foo.bar", "sensor.one.name");
    private static final List<String> SENSOR_BLACKLIST = ImmutableList.of("host.name");
    private static final List<String> ENTITY_WHITELIST = ImmutableList.of("test_type");
    private static final List<String> ENTITY_BLACKLIST = ImmutableList.of("test_type_2");

    private static final Map<String, Object> TEST_RESULT = Maps.newHashMap();

    private static final Map<String, Object> EXPECTED_CREDENTIALS = Maps.newHashMap();


    static {
        Map<String, Object> testResultChild = Maps.newHashMap();
        testResultChild.put("sensor.one.name", "");
        testResultChild.put("sensor.two.name", "");
        testResultChild.put("host.name", "");

        TEST_RESULT.put("name", testResultChild);

        Map<String, Object> expectedCredentialsChild = Maps.newHashMap();
        expectedCredentialsChild.put("sensor.one.name", "");

        EXPECTED_CREDENTIALS.putAll(expectedCredentialsChild);
    }

    @Mock
    private BrooklynApi restApi;

    @Mock
    private SensorApi sensorApi;

    @Mock
    private EntityApi entityApi;

    @InjectMocks
    private BrooklynRestAdmin brooklynRestAdmin;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetApplicationSensors() throws ExecutionException, InterruptedException {
        when(restApi.getSensorApi()).thenReturn(sensorApi);
        when(restApi.getEntityApi()).thenReturn(entityApi);
        when(restApi.getSensorApi().list(Mockito.anyString(), Mockito.eq("test_id"))).thenReturn(TEST_SENSOR_SUMMARIES);
        when(restApi.getEntityApi().list(Mockito.any(String.class))).thenReturn(TEST_ENTITY_SUMMARIES);
        when(restApi.getEntityApi().getChildren(Mockito.anyString(), Mockito.anyString())).thenReturn(ImmutableList.of());
        when(sensorApi.get(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), anyBoolean())).thenReturn("");
        Future<Map<String, Object>> applicationSensors = brooklynRestAdmin.getApplicationSensors("test-application");
        Map<String, Object> sensors = applicationSensors.get();

        assertEquals(TEST_RESULT, sensors);
    }

    @Test
    public void testGetCredentialsFromSensors() throws ExecutionException, InterruptedException {
        when(restApi.getSensorApi()).thenReturn(sensorApi);
        when(restApi.getEntityApi()).thenReturn(entityApi);
        when(restApi.getSensorApi().list(Mockito.anyString(), Mockito.eq("test-application"))).thenReturn(TEST_SENSOR_SUMMARIES);
        when(restApi.getEntityApi().list(Mockito.any(String.class))).thenReturn(TEST_ENTITY_SUMMARIES);
        when(restApi.getEntityApi().getChildren(Mockito.anyString(), Mockito.anyString())).thenReturn(ImmutableList.of());
        when(sensorApi.get(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), anyBoolean())).thenReturn("");

        Future<Map<String, Object>> credentialsFuture = brooklynRestAdmin.getCredentialsFromSensors("test-application", "test-application", s -> SENSOR_WHITELIST.contains(s), s-> !SENSOR_BLACKLIST.contains(s), e-> ENTITY_WHITELIST.contains(e), e -> !ENTITY_BLACKLIST.contains(e));
        Map<String, Object> credentials = credentialsFuture.get();

        assertEquals(EXPECTED_CREDENTIALS, credentials);
    }

    @Test
    public void testBlacklistEntitiesWhileGettingCredentials() throws ExecutionException, InterruptedException {
        when(restApi.getSensorApi()).thenReturn(sensorApi);
        when(restApi.getEntityApi()).thenReturn(entityApi);
        when(restApi.getSensorApi().list(Mockito.anyString(), Mockito.anyString())).thenReturn(TEST_SENSOR_SUMMARIES);
        when(restApi.getEntityApi().list(Mockito.anyString())).thenReturn(TEST_ENTITY_SUMMARIES);
        when(restApi.getEntityApi().getChildren(Mockito.eq("test-application"), Mockito.eq("test-application"))).thenReturn(TEST_ENTITY_SUMMARIES);
        when(restApi.getEntityApi().getChildren(Mockito.anyString(), Mockito.eq("test_id"))).thenReturn(TEST_CHILD_ENTITY_SUMMARIES);
        when(sensorApi.get(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), anyBoolean())).thenReturn("");

        List<String> entityBlacklist = ImmutableList.of("test_type_2");
        Future<Map<String, Object>> credentialsFuture = brooklynRestAdmin.getCredentialsFromSensors("test-application", "test-application", s -> true, s-> true, e-> true, e -> !entityBlacklist.contains(e));
        Map<String, Object> credentials = credentialsFuture.get();

        Map<String, Object> expected = Maps.newHashMap();
        Map<String, Object> expectedCredentialsChild = Maps.newHashMap();
        expectedCredentialsChild.put("sensor.one.name", "");
        expectedCredentialsChild.put("sensor.two.name", "");
        expectedCredentialsChild.put("host.name", "");
        expected.putAll(expectedCredentialsChild);
        expected.put("children", ImmutableMap.of("child", expectedCredentialsChild));
        assertEquals(expected, credentials);
    }
    
    @Test
    public void testWhitelistEntitiesWhileGettingCredentials() throws ExecutionException, InterruptedException {
        when(restApi.getSensorApi()).thenReturn(sensorApi);
        when(restApi.getEntityApi()).thenReturn(entityApi);
        when(restApi.getSensorApi().list(Mockito.anyString(), Mockito.anyString())).thenReturn(TEST_SENSOR_SUMMARIES);
        when(restApi.getEntityApi().list(Mockito.anyString())).thenReturn(TEST_ENTITY_SUMMARIES);
        when(restApi.getEntityApi().getChildren(Mockito.eq("test-application"), Mockito.eq("test-application"))).thenReturn(TEST_ENTITY_SUMMARIES);
        when(restApi.getEntityApi().getChildren(Mockito.anyString(), Mockito.eq("test_id"))).thenReturn(TEST_CHILD_ENTITY_SUMMARIES);
        when(sensorApi.get(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), anyBoolean())).thenReturn("");

        List<String> entityWhitelist = ImmutableList.of("test_type");
        Future<Map<String, Object>> credentialsFuture = brooklynRestAdmin.getCredentialsFromSensors("test-application", "test-application", s -> true, s-> true, e-> entityWhitelist.contains(e), e -> true);
        Map<String, Object> credentials = credentialsFuture.get();
        
        Map<String, Object> expected = Maps.newHashMap();
        Map<String, Object> expectedCredentialsChild = Maps.newHashMap();
        expectedCredentialsChild.put("sensor.one.name", "");
        expectedCredentialsChild.put("sensor.two.name", "");
        expectedCredentialsChild.put("host.name", "");
        expected.putAll(expectedCredentialsChild);
        expected.put("children", ImmutableMap.of("child", expectedCredentialsChild));
        
        assertEquals(expected, credentials);
    }

}
