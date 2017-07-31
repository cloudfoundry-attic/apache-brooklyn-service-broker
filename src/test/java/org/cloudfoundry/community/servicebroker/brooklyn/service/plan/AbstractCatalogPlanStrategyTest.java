package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.brooklyn.rest.domain.CatalogItemSummary;
import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.annotation.AsyncResult;

public class AbstractCatalogPlanStrategyTest {

    private static final List<CatalogItemSummary> CATALOG_ITEM_SUMMARIES = Arrays.asList(
            new CatalogItemSummary("test_name", "1.0", "","test_name", "foo",  "", "{}", "1.0", "", null, false, null),
            new CatalogItemSummary("test_name", "1.1", "","test_name", "foo",  "", "{}","1.1", "", null, false, null),
            new CatalogItemSummary("test_name", "1.2", "","test_name", "foo",  "","{brooklyn.config: {broker.config: {hidden: true}}}", "1.1", "", null, false, null),
            new CatalogItemSummary("test_name_2", "1.2", "","test_name_2", "foo", "", "{brooklyn.config: {broker.config: {hidden: true}}}", "1.2", "", null, false, null)
    );

    private static final CatalogItemSummary TEST_SUMMARY_WITH_METADATA = new CatalogItemSummary("test_name", "1.2", "","test_name", "foo",  "","{brooklyn.config: {broker.config: {metadata: {test: \"test value\", brooklynCatalogId: \"test\"}}}}", "1.2", "", null, false, null);

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
        when(admin.getCatalogApplications(Mockito.anyBoolean())).thenReturn(new AsyncResult<>(CATALOG_ITEM_SUMMARIES));
        when(brooklynConfig.includesAllCatalogVersions()).thenReturn(false);
        List<ServiceDefinition> serviceDefinitions = catalogPlanStrategy.makeServiceDefinitions();
        assertEquals(2, serviceDefinitions.size());
        assertEquals("1.1", serviceDefinitions.get(1).getDescription());
        verify(admin, never()).getIconAsBase64(anyString());
    }

    @Test
    public void testMakeServiceDefinitionsAllVersions() {
        when(admin.getCatalogApplications(Mockito.anyBoolean())).thenReturn(new AsyncResult<>(CATALOG_ITEM_SUMMARIES));
        when(brooklynConfig.includesAllCatalogVersions()).thenReturn(true);
        List<ServiceDefinition> serviceDefinitions = catalogPlanStrategy.makeServiceDefinitions();
        assertEquals(3, serviceDefinitions.size());
        verify(admin, never()).getIconAsBase64(anyString());
    }

    @Test
    public void testMetadataFromBlueprint() {
        when(admin.getCatalogApplications(Mockito.anyBoolean())).thenReturn(new AsyncResult<>(Arrays.asList(TEST_SUMMARY_WITH_METADATA)));
        when(brooklynConfig.includesAllCatalogVersions()).thenReturn(false);
        List<ServiceDefinition> serviceDefinitions = catalogPlanStrategy.makeServiceDefinitions();
        String expectedKey = "test";
        String expectedValue = "test value";
        Map<String, Object> metadata = serviceDefinitions.get(1).getMetadata();
        assertTrue(metadata.containsKey(expectedKey));
        assertEquals(expectedValue, metadata.get(expectedKey));

        expectedKey = "brooklynCatalogId";
        expectedValue = TEST_SUMMARY_WITH_METADATA.getId();
        assertTrue(metadata.containsKey(expectedKey));
        assertEquals(expectedValue, metadata.get(expectedKey));
    }
}
