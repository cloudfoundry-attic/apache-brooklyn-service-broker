package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.apache.brooklyn.rest.domain.CatalogItemSummary;
import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.annotation.AsyncResult;

public class AbstractCatalogPlanStrategyTest {

    private static final List<CatalogItemSummary> CATALOG_ITEM_SUMMARIES = Arrays.asList(
            new CatalogItemSummary("test_name", "1.0", "test_name", "foo", "", "1.0", "", false, null),
            new CatalogItemSummary("test_name", "1.1", "test_name", "foo", "", "1.1", "", false, null)
    );

    @InjectMocks
    private TestAbstractCatalogPlanStrategyImpl catalogPlanStrategy;
    @Mock
    private BrooklynRestAdmin admin;
    @Mock
    private BrooklynConfig brooklynConfig;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMakeServiceDefinitionsLatestVersions() {
        when(admin.getCatalogApplications()).thenReturn(new AsyncResult<>(CATALOG_ITEM_SUMMARIES));
        when(brooklynConfig.isAllCatalogVersions()).thenReturn(false);
        List<ServiceDefinition> serviceDefinitions = catalogPlanStrategy.makeServiceDefinitions();
        assertEquals(1, serviceDefinitions.size());
        assertEquals("1.1", serviceDefinitions.get(0).getDescription());
    }

    @Test
    public void testMakeServiceDefinitionsAllVersions() {
        when(admin.getCatalogApplications()).thenReturn(new AsyncResult<>(CATALOG_ITEM_SUMMARIES));
        when(brooklynConfig.isAllCatalogVersions()).thenReturn(true);
        List<ServiceDefinition> serviceDefinitions = catalogPlanStrategy.makeServiceDefinitions();
        assertEquals(2, serviceDefinitions.size());
    }
}
