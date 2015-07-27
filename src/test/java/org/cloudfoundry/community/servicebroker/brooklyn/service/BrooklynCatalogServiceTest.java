package org.cloudfoundry.community.servicebroker.brooklyn.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.community.servicebroker.brooklyn.BrooklynConfiguration;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.CatalogPlanStrategy;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.LocationPlanStrategy;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import brooklyn.rest.domain.LocationSummary;

import com.google.common.collect.ImmutableMap;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {BrooklynConfiguration.class})
public class BrooklynCatalogServiceTest {
    
    private static final List<LocationSummary> LOCATION_SUMMARIES = Arrays.asList(
            new LocationSummary("test_id", "test_name", "spec", "", ImmutableMap.of(), ImmutableMap.of()),
            new LocationSummary("test_id2", "test_name2", "spec2", "", ImmutableMap.of(), ImmutableMap.of())
            );

    @InjectMocks
    private BrooklynCatalogService brooklynCatalogService;
    @Mock
    private BrooklynRestAdmin admin;
    @Mock
    private CatalogPlanStrategy catalogPlanStrategy;
    
    
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void testGetLocationPlans(){
        brooklynCatalogService.setPlanStrategy(new LocationPlanStrategy(admin));
        when(admin.getLocations()).thenReturn(LOCATION_SUMMARIES);
        List<Plan> plans = brooklynCatalogService.getPlans("test_id", "");
        
        assertEquals(LOCATION_SUMMARIES.size(), plans.size());
    }

}
