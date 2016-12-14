package org.cloudfoundry.community.servicebroker.brooklyn.repository;


import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.brooklyn.rest.api.ApplicationApi;
import org.apache.brooklyn.rest.client.BrooklynApi;
import org.apache.brooklyn.rest.domain.ApplicationSummary;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class RepositoriesTest {

    @Mock
    private BrooklynApi brooklynApi;

    @Mock
    private ApplicationApi applicationApi;

    @Mock
    private ApplicationSummary applicationSummary;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(brooklynApi.getApplicationApi()).thenReturn(applicationApi);
    }

    @Test
    @Ignore
    // TODO: This test does not work as the ApplicationApi.get() signature does not explicitly says that it can throw an exception.
    public void testCreateRepositoriesCreatesItIfRepositoryEntityDoesNotExist() {
        when(applicationApi.get(anyString())).thenThrow(new Exception());

        Repositories.createRepositories(brooklynApi);

        verify(applicationApi).createFromForm(anyString());
    }

    @Test
    public void testCreateRepositoriesDoesNotIfRepositoryEntityExist() {
        when(applicationApi.get(anyString())).thenReturn(applicationSummary);

        Repositories.createRepositories(brooklynApi);

        verify(applicationApi, never()).createFromForm(anyString());
    }
}
