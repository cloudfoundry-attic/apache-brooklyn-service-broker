package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.base.Joiner;

public class SizePlanStrategyTest {

    @InjectMocks
    SizePlanStrategy strategy;

    @Mock
    BrooklynConfig brooklynConfig;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDefaultSizes() {
        when(brooklynConfig.getLocation()).thenReturn("aws-ec2:eu-west-1");
        String yaml = Joiner.on("\n").join(
                "services:",
                "- serviceType: brooklyn.entity.basic.BasicApplication",
                "brooklyn.config:",
                "  broker.config:",
                "    plans:",
                "      small:",
                "        provisioning.properties:",
                "          minCores: 1",
                "          minRam: 1",
                "      medium:",
                "        provisioning.properties:",
                "          minCores: 2",
                "          minRam: 2",
                "      large:",
                "        provisioning.properties:",
                "          minCores: 4",
                "          minRam: 4");

        List<Plan> plans = strategy.makePlans("test_id", yaml);
        assertEquals(3, plans.size());
        assertEquals("small", plans.get(0).getName());
        assertEquals("medium", plans.get(1).getName());
        assertEquals("large", plans.get(2).getName());
        Map<String, Object> planMetadata = (Map<String, Object>)(plans.get(0).getMetadata().get("provisioning.properties"));
        assertEquals((Integer)1, Integer.valueOf(planMetadata.get("minCores").toString()));
        assertEquals((Integer)1, Integer.valueOf(planMetadata.get("minRam").toString()));
    }

}
