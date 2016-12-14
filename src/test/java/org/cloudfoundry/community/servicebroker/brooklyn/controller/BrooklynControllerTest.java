package org.cloudfoundry.community.servicebroker.brooklyn.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.community.servicebroker.brooklyn.model.BrooklynServiceInstance;
import org.cloudfoundry.community.servicebroker.brooklyn.repository.BrooklynServiceInstanceRepository;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.ImmutableMap;

public class BrooklynControllerTest {

    @Mock
    private BrooklynServiceInstanceRepository instanceRepository;

    @Mock
    private BrooklynRestAdmin admin;

    @Mock
    private Future<Map<String, Object>> futureMap;

    @Mock
    private Future<Boolean> futureBoolean;

    private BrooklynController brooklynController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        brooklynController = new BrooklynController(admin, instanceRepository);
    }

    @Test
    public void testCreate() throws IOException {
        final String source = "Hello world";
        final InputStream inputStream = IOUtils.toInputStream(source, "UTF-8");

        brooklynController.create(inputStream);

        verify(admin).postBlueprint(source);
    }

    @Test
    public void testDeleteDoesNotThrowsException() throws Exception {
        final String name = "foo";
        final String version = "1.0.0";

        Mockito.doThrow(new Exception()).when(admin).deleteCatalogEntry(any(String.class), any(String.class));

        brooklynController.delete(name, version);
    }

    @Test
    public void testDelete() throws Exception {
        final String name = "foo";
        final String version = "1.0.0";

        brooklynController.delete(name, version);

        verify(admin).deleteCatalogEntry(name, version);
    }

    @Test
    public void testInvokeReturnsObjectIfNoInstance() {
        final String application = "foo";
        final String entity = "bar";
        final String effector = "zoo";
        final Map<String, Object> params = ImmutableMap.of();

        when(instanceRepository.findOne(any(String.class))).thenReturn(null);

        final Object response = brooklynController.invoke(application, entity, effector, params);

        assertTrue(response instanceof Object);
    }

    @Test
    public void testInvokeReturnsObjectIfInstance() {
        final String appId = "hello";
        final String serviceId = "world";
        final String application = "foo";
        final String entity = "bar";
        final String effector = "zoo";
        final Map<String, Object> params = ImmutableMap.of();

        when(instanceRepository.findOne(any(String.class))).thenReturn(new BrooklynServiceInstance(appId, serviceId));

        brooklynController.invoke(application, entity, effector, params);

        verify(admin).invokeEffector(serviceId, entity, effector, params);
    }

    @Test
    public void testEffectorsReturnsEmptyCollectionIfNoInstance() {
        final String application = "foo";

        when(instanceRepository.findOne(any(String.class))).thenReturn(null);

        final Map<String, Object> effectors = brooklynController.effectors(application);

        assertTrue(effectors.isEmpty());
    }

    @Test
    public void testEffectorsReturnsCollectionIfInstance() throws ExecutionException, InterruptedException {
        final String appId = "hello";
        final String serviceId = "world";
        final String application = "foo";
        final ImmutableMap<String, Object> effectorsObject = ImmutableMap.of("foo", "bar", "hello", "world");

        when(instanceRepository.findOne(any(String.class))).thenReturn(new BrooklynServiceInstance(appId, serviceId));
        when(futureMap.get()).thenReturn(effectorsObject);
        when(admin.getApplicationEffectors(any(String.class))).thenReturn(futureMap);

        final Map<String, Object> effectors = brooklynController.effectors(application);

        assertFalse(effectors.isEmpty());
        assertEquals(effectors, effectorsObject);
    }

    @Test
    public void testSensorsReturnsEmptyCollectionIfNoInstance() {
        final String application = "foo";

        when(instanceRepository.findOne(any(String.class))).thenReturn(null);

        final Map<String, Object> effectors = brooklynController.sensors(application);

        assertTrue(effectors.isEmpty());
    }

    @Test
    public void testSensorsReturnsCollectionIfInstance() throws ExecutionException, InterruptedException {
        final String appId = "hello";
        final String serviceId = "world";
        final String application = "foo";
        final ImmutableMap<String, Object> effectorsObject = ImmutableMap.of("foo", "bar", "hello", "world");

        when(instanceRepository.findOne(any(String.class))).thenReturn(new BrooklynServiceInstance(appId, serviceId));
        when(futureMap.get()).thenReturn(effectorsObject);
        when(admin.getApplicationSensors(any(String.class))).thenReturn(futureMap);

        final Map<String, Object> effectors = brooklynController.sensors(application);

        assertFalse(effectors.isEmpty());
        assertEquals(effectors, effectorsObject);
    }

    @Test
    public void testIsRunningReturnsFalseIfNoInstance() {
        final String application = "foo";

        when(instanceRepository.findOne(any(String.class))).thenReturn(null);

        final Boolean isRunning = brooklynController.isRunning(application);

        assertFalse(isRunning);
    }

    @Test
    public void testSensorsReturnsSensorValueIfInstance() throws ExecutionException, InterruptedException {
        final String appId = "hello";
        final String serviceId = "world";
        final String application = "foo";
        final Boolean isRunningObject = true;
        final ImmutableMap<String, Object> effectorsObject = ImmutableMap.of("foo", "bar", "hello", "world");

        when(instanceRepository.findOne(any(String.class))).thenReturn(new BrooklynServiceInstance(appId, serviceId));
        when(futureBoolean.get()).thenReturn(isRunningObject);
        when(admin.isApplicationRunning(any(String.class))).thenReturn(futureBoolean);

        final Boolean isRunning = brooklynController.isRunning(application);

        assertEquals(isRunning, isRunningObject);
    }
}
