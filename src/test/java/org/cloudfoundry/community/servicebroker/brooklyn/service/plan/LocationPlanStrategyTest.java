package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.apache.brooklyn.rest.domain.LocationSummary;
import org.apache.brooklyn.util.yaml.Yamls;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.springframework.cloud.servicebroker.model.Plan;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.annotation.AsyncResult;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

public class LocationPlanStrategyTest {

    private static final List<LocationSummary> LOCATION_SUMMARIES = Arrays.asList(
            new LocationSummary("test_id", "test_name", "spec", "", ImmutableMap.of(), null, ImmutableMap.of()),
            new LocationSummary("test_id2", "test_name2", "spec2", "", ImmutableMap.of(), null, ImmutableMap.of())
    );

    private static final String TEST_ID = "test_id";
    private static final String TEST_APP = "Test App";

    @InjectMocks
    private LocationPlanStrategy locationPlanStrategy;

    @Mock
    private BrooklynRestAdmin admin;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMakePlansNoLocationsInYAML() {
        when(admin.getLocations()).thenReturn(new AsyncResult<>(LOCATION_SUMMARIES));
        List<Plan> plans = locationPlanStrategy.makePlans(TEST_ID, TEST_APP, "the_yaml");
        assertEquals(LOCATION_SUMMARIES.size(), plans.size());
        checkLocationSummariesEqualsPlan(LOCATION_SUMMARIES.get(0), plans.get(0));
        checkLocationSummariesEqualsPlan(LOCATION_SUMMARIES.get(1), plans.get(1));
    }

    @Test
    public void testMakePlansStringLocationInYAML() {
        String yaml = Joiner.on("\n").join(
                "location: aws-ec2:eu-west-1",
                "services:",
                "- serviceType: brooklyn.entity.basic.BasicApplication");
        Object rootObject = Yamls.parseAll(yaml).iterator().next();
        List<Plan> plans = locationPlanStrategy.makePlans(TEST_ID, TEST_APP, rootObject);
        assertEquals(plans.size(), 1);
        assertEquals(plans.get(0).getId(), TEST_ID + "." + "aws-ec2:eu-west-1");
        assertEquals(plans.get(0).getName(), "aws-ec2:eu-west-1");

    }

    @Test
    public void testMakePlansMapLocationInYAML() {
        String yaml = Joiner.on("\n").join(
                "location: ",
                "  jclouds:aws-ec2: ",
                "    region: us-east-1",
                "services:",
                "- serviceType: brooklyn.entity.basic.BasicApplication");
        Object rootObject = Yamls.parseAll(yaml).iterator().next();
        List<Plan> plans = locationPlanStrategy.makePlans(TEST_ID, TEST_APP, rootObject);
        assertEquals(plans.size(), 1);
        assertEquals(plans.get(0).getId(), TEST_ID + "." + "jclouds:aws-ec2");
        assertEquals(plans.get(0).getName(), "jclouds:aws-ec2");
    }

    private void checkLocationSummariesEqualsPlan (LocationSummary locationSummary, Plan plan) {
        assertEquals( TEST_ID + "." + locationSummary.getName(), plan.getId());
        assertEquals(locationSummary.getName(), plan.getName());
    }
}
