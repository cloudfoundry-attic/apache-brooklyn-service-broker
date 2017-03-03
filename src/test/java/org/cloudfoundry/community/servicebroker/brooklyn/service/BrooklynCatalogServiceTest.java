package org.cloudfoundry.community.servicebroker.brooklyn.service;

import com.google.common.collect.ImmutableMap;
import org.apache.brooklyn.rest.domain.CatalogItemSummary;
import org.apache.brooklyn.rest.domain.LocationSummary;
import org.apache.brooklyn.util.core.ResourceUtils;
import org.cloudfoundry.community.servicebroker.brooklyn.BrooklynConfiguration;
import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.CatalogPlanStrategy;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.LocationPlanStrategy;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.PlaceholderReplacer;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.SizePlanStrategy;
import org.springframework.cloud.servicebroker.model.Catalog;
import org.springframework.cloud.servicebroker.model.Plan;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {BrooklynConfiguration.class})
public class BrooklynCatalogServiceTest {
    
    private static final List<LocationSummary> LOCATION_SUMMARIES = Arrays.asList(
            new LocationSummary("test_id", "test_name", "spec", "", ImmutableMap.of(), null, ImmutableMap.of()),
            new LocationSummary("test_id2", "test_name2", "spec2", "", ImmutableMap.of(), null, ImmutableMap.of())
            );

    private static final String YAML = ResourceUtils.create().getResourceAsString("plans-catalog.yaml");
    private static final String YAML_2 = ResourceUtils.create().getResourceAsString("noplans-catalog.yaml");

    private static final List<CatalogItemSummary> CATALOG_ITEM_SUMMARIES = Arrays.asList(
            new CatalogItemSummary("test_name", "1.0", "test_name", "foo", "", YAML, "", "", null, false, null),
            new CatalogItemSummary("test_name", "1.0", "test_name", "foo", "", YAML_2, "", "", null, false, null)
    );

    @InjectMocks
    private BrooklynCatalogService brooklynCatalogService;
    @Mock
    private BrooklynRestAdmin admin;
    @Mock
    private CatalogPlanStrategy catalogPlanStrategy;
    @Mock
    private BrooklynConfig brooklynConfig;
    @Mock 
    private PlaceholderReplacer replacer;
    
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void testGetLocationPlans(){
        brooklynCatalogService.setPlanStrategy(new LocationPlanStrategy(admin, replacer, brooklynConfig));
        when(admin.getLocations()).thenReturn(new AsyncResult<>(LOCATION_SUMMARIES));
        List<Plan> plans = brooklynCatalogService.getPlans("test_id", "");
        
        assertEquals(LOCATION_SUMMARIES.size(), plans.size());
    }

    @Test
    public void testGetServicesWithSizeStrategy() {
        brooklynCatalogService.setPlanStrategy(new SizePlanStrategy(admin, brooklynConfig, replacer));
        when(replacer.replaceValues(Mockito.anyMap())).thenCallRealMethod();
        when(admin.getCatalogApplications(Mockito.anyBoolean())).thenReturn(new AsyncResult<>(CATALOG_ITEM_SUMMARIES));
        Catalog catalog = brooklynCatalogService.getCatalog();
        List<ServiceDefinition> serviceDefinitions = catalog.getServiceDefinitions();
        assertEquals(2, serviceDefinitions.size());
    }

}
