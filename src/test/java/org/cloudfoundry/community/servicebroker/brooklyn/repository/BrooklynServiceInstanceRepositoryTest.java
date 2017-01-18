package org.cloudfoundry.community.servicebroker.brooklyn.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.cloudfoundry.community.servicebroker.brooklyn.model.BrooklynServiceInstance;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.servicebroker.model.OperationState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class BrooklynServiceInstanceRepositoryTest {

    @Mock
    private BrooklynRestAdmin brooklynRestAdmin;

    @Mock
    private Future<Map<String, Object>> futureConfigAsMap;

    @Mock
    private Future<String> futureServiceState;

    @Mock
    private Future<Object> futureConfig;


    private BrooklynServiceInstanceRepository brooklynServiceInstanceRepository;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(brooklynRestAdmin.getConfigAsMap(anyString(), anyString(), anyString())).thenReturn(futureConfigAsMap);
        when(brooklynRestAdmin.getServiceState(anyString())).thenReturn(futureServiceState);
        when(brooklynRestAdmin.setConfig(anyString(), anyString(), anyString(), anyString())).thenReturn(futureConfig);
        brooklynServiceInstanceRepository = new BrooklynServiceInstanceRepository(brooklynRestAdmin);
    }

    @Test
    public void testFindOneReturnsNullIfConfigIsNull() throws ExecutionException, InterruptedException {
        final ImmutableMap<String, Object> config = null;
        final String serviceInstanceId = "foo";

        when(futureConfigAsMap.get()).thenReturn(config);

        assertNull(brooklynServiceInstanceRepository.findOne(serviceInstanceId));
    }

    @Test
    public void testFindOneReturnsBrooklynServiceInstanceIfNoEntityId() throws ExecutionException, InterruptedException {
        final ImmutableMap<String, Object> config = ImmutableMap.of();
        final String serviceInstanceId = "foo";

        when(futureConfigAsMap.get()).thenReturn(config);

        final BrooklynServiceInstance brooklynServiceInstance = brooklynServiceInstanceRepository.findOne(serviceInstanceId);

        assertEquals(serviceInstanceId, brooklynServiceInstance.getServiceInstanceId());
        assertEquals("null", brooklynServiceInstance.getServiceDefinitionId());
        assertEquals("null", brooklynServiceInstance.getPlanId());
        assertEquals("null", brooklynServiceInstance.getEntityId());
        assertEquals("null", brooklynServiceInstance.getOperation());
        assertEquals(OperationState.FAILED, brooklynServiceInstance.getOperationState());
        verify(brooklynRestAdmin, never()).deleteConfig(anyString(), anyString(), anyString());
        verify(brooklynRestAdmin, times(1)).setConfig("service-broker-records","service-instance-repository", serviceInstanceId, brooklynServiceInstance);
    }

    @Test
    public void testFindOneReturnsBrooklynServiceInstanceIfNoPlanId() throws ExecutionException, InterruptedException {
        final ImmutableMap<String, Object> config = ImmutableMap.of();
        final String serviceInstanceId = "foo";

        when(futureConfigAsMap.get()).thenReturn(config);

        final BrooklynServiceInstance brooklynServiceInstance = brooklynServiceInstanceRepository.findOne(serviceInstanceId);

        assertEquals(serviceInstanceId, brooklynServiceInstance.getServiceInstanceId());
        assertEquals("null", brooklynServiceInstance.getServiceDefinitionId());
        assertEquals("null", brooklynServiceInstance.getPlanId());
        assertEquals("null", brooklynServiceInstance.getEntityId());
        assertEquals("null", brooklynServiceInstance.getOperation());
        assertEquals(OperationState.FAILED, brooklynServiceInstance.getOperationState());
        verify(brooklynRestAdmin, never()).deleteConfig(anyString(), anyString(), anyString());
        verify(brooklynRestAdmin,times(1)).setConfig("service-broker-records","service-instance-repository", serviceInstanceId, brooklynServiceInstance);
    }

    @Test
    public void testFindOneReturnsBrooklynServiceInstanceDoesNotIncludeEverything() throws ExecutionException, InterruptedException {
        final ImmutableMap<String, Object> config = ImmutableMap.of();
        final String serviceInstanceId = "foo";

        when(futureConfigAsMap.get()).thenReturn(config);

        final BrooklynServiceInstance brooklynServiceInstance = brooklynServiceInstanceRepository.findOne(serviceInstanceId, false);

        assertEquals(serviceInstanceId, brooklynServiceInstance.getServiceInstanceId());
        assertEquals("null", brooklynServiceInstance.getServiceDefinitionId());
        assertEquals("null", brooklynServiceInstance.getPlanId());
        assertEquals("null", brooklynServiceInstance.getEntityId());
        assertNull(brooklynServiceInstance.getOperation());
        assertNull(brooklynServiceInstance.getOperationState());
        verify(brooklynRestAdmin, never()).deleteConfig(anyString(), anyString(), anyString());
        verify(brooklynRestAdmin, never()).setConfig(anyString(), anyString(), anyString(), anyObject());
    }

    @Test
    public void testFindOneReturnsStateInProgressWithCreatingLastOperation() throws ExecutionException, InterruptedException {
        final ImmutableMap<String, Object> config = ImmutableMap.of("operation", "CREATING");
        final String serviceInstanceId = "foo";

        when(futureConfigAsMap.get()).thenReturn(config);

        final ImmutableList<String> states = ImmutableList.of("CREATED", "STARTING");
        for (String state : states) {
            when(futureServiceState.get()).thenReturn(state);

            final BrooklynServiceInstance brooklynServiceInstance = brooklynServiceInstanceRepository.findOne(serviceInstanceId);

            assertEquals(OperationState.IN_PROGRESS, brooklynServiceInstance.getOperationState());
        }
    }

    @Test
    public void testFindOneReturnsStateSucceededWithCreatingLastOperation() throws ExecutionException, InterruptedException {
        final ImmutableMap<String, Object> config = ImmutableMap.of("operation", "CREATING");
        final String serviceInstanceId = "foo";

        when(futureConfigAsMap.get()).thenReturn(config);
        when(futureServiceState.get()).thenReturn("RUNNING");

        final BrooklynServiceInstance brooklynServiceInstance = brooklynServiceInstanceRepository.findOne(serviceInstanceId);

        assertEquals("null", OperationState.SUCCEEDED, brooklynServiceInstance.getOperationState());
    }

    @Test
    public void testFindOneReturnsStateInProgressWithDeletingLastOperation() throws ExecutionException, InterruptedException {
        final ImmutableMap<String, Object> config = ImmutableMap.of("operation", "DELETING");
        final String serviceInstanceId = "foo";

        when(futureConfigAsMap.get()).thenReturn(config);

        final ImmutableList<String> states = ImmutableList.of("STOPPED", "STOPPING");
        for (String state : states) {
            when(futureServiceState.get()).thenReturn(state);

            final BrooklynServiceInstance brooklynServiceInstance = brooklynServiceInstanceRepository.findOne(serviceInstanceId);

            assertEquals(OperationState.IN_PROGRESS, brooklynServiceInstance.getOperationState());
        }
    }

    @Test
    public void testFindOneReturnsStateSucceededWithDeletingLastOperation() throws ExecutionException, InterruptedException {
        final ImmutableMap<String, Object> config = ImmutableMap.of("operation", "DELETING");
        final String serviceInstanceId = "foo";

        when(futureConfigAsMap.get()).thenReturn(config);
        when(futureServiceState.get()).thenReturn("DESTROYED");

        final BrooklynServiceInstance brooklynServiceInstance = brooklynServiceInstanceRepository.findOne(serviceInstanceId);

        assertEquals("null", OperationState.SUCCEEDED, brooklynServiceInstance.getOperationState());

        verify(brooklynRestAdmin, times(1)).deleteConfig(anyString(), anyString(), anyString());
        verify(brooklynRestAdmin, never()).setConfig(anyString(), anyString(), anyString(), anyObject());
    }
}
