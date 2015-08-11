package org.cloudfoundry.community.servicebroker.brooklyn.service;

import static org.junit.Assert.assertEquals;
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

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class BrooklynRestAdminTest {

    private static final List<EntitySummary> TEST_ENTITY_SUMMARIES = ImmutableList.of(
            new EntitySummary("test_id1", "name", "test_type", "test_catalog_item_id", ImmutableMap.of())
    );

    private static final List<SensorSummary> TEST_SENSOR_SUMMARIES_1 = ImmutableList.of(
            new SensorSummary("sensor.one.name", "sensor.one.type", "sensor.one.description", ImmutableMap.of()),
            new SensorSummary("sensor.two.name", "sensor.two.type", "sensor.two.description", ImmutableMap.of()),
            new SensorSummary("host.name", "sensor.two.type", "myHostName", ImmutableMap.of())
    );

    private static final List<String> SENSOR_WHITELIST = ImmutableList.of("foo.bar", "sensor.one.name");

    private static final Map<String, Object> TEST_RESULT = Maps.newHashMap();

    private static final Map<String, Object> EXPECTED_CREDENTIALS = Maps.newHashMap();

    static {
        Map<String, Object> testResultChild = Maps.newHashMap();
        testResultChild.put("children", ImmutableMap.of());
        testResultChild.put("sensor.one.name", null);
        testResultChild.put("sensor.two.name", null);
        testResultChild.put("host.name", null);

        TEST_RESULT.put("name", testResultChild);

        Map<String, Object> expectedCredentialsChild = Maps.newHashMap();
        expectedCredentialsChild.put("children", ImmutableMap.of());
        expectedCredentialsChild.put("sensor.one.name", null);
        expectedCredentialsChild.put("host.name", null);

        EXPECTED_CREDENTIALS.put("name", expectedCredentialsChild);
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
        when(restApi.getSensorApi().list(Mockito.anyString(), Mockito.eq("test_id1"))).thenReturn(TEST_SENSOR_SUMMARIES_1);
        when(restApi.getEntityApi().list(Mockito.any(String.class))).thenReturn(TEST_ENTITY_SUMMARIES);
        when(restApi.getEntityApi().getChildren(Mockito.anyString(), Mockito.anyString())).thenReturn(ImmutableList.of());

        Future<Map<String, Object>> applicationSensors = brooklynRestAdmin.getApplicationSensors("test-application");
        Map<String, Object> sensors = applicationSensors.get();

        assertEquals(TEST_RESULT, sensors);
    }

    @Test
    public void testGetCredentialsFromSensors() throws ExecutionException, InterruptedException {
        when(restApi.getSensorApi()).thenReturn(sensorApi);
        when(restApi.getEntityApi()).thenReturn(entityApi);
        when(restApi.getSensorApi().list(Mockito.anyString(), Mockito.eq("test_id1"))).thenReturn(TEST_SENSOR_SUMMARIES_1);
        when(restApi.getEntityApi().list(Mockito.any(String.class))).thenReturn(TEST_ENTITY_SUMMARIES);
        when(restApi.getEntityApi().getChildren(Mockito.anyString(), Mockito.anyString())).thenReturn(ImmutableList.of());

        Future<Map<String, Object>> credentialsFuture = brooklynRestAdmin.getCredentialsFromSensors("test-application",
                s -> SENSOR_WHITELIST.contains(s));
        Map<String, Object> credentials = credentialsFuture.get();

        assertEquals(EXPECTED_CREDENTIALS, credentials);
    }

}
