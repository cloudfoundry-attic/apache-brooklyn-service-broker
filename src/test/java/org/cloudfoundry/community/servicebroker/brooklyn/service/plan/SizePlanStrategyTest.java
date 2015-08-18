package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.common.base.Joiner;

public class SizePlanStrategyTest {

    @InjectMocks
    private SizePlanStrategy strategy;

    @Mock
    private BrooklynConfig brooklynConfig;
    @Mock
    private PlaceholderReplacer replacer;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDefaultSizes() {
        when(brooklynConfig.getLocation()).thenReturn("aws-ec2:eu-west-1");
        when(replacer.replaceValue(Mockito.anyString())).thenCallRealMethod();
        when(replacer.replaceValues(Mockito.anyList())).thenCallRealMethod();
        when(replacer.replaceValues(Mockito.anyMap())).thenCallRealMethod();
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
    
    @Test
    public void testDefaultSizesWithDescription() {
        when(brooklynConfig.getLocation()).thenReturn("aws-ec2:eu-west-1");
        when(replacer.randomString(8)).thenReturn("password");
        when(replacer.replaceValue(Mockito.anyString())).thenCallRealMethod();
        when(replacer.replaceValues(Mockito.anyList())).thenCallRealMethod();
        when(replacer.replaceValues(Mockito.anyMap())).thenCallRealMethod();
        String yaml = Joiner.on("\n").join(
                "services:",
                "- serviceType: brooklyn.entity.basic.BasicApplication",
                "brooklyn.config:",
                "  broker.config:",
                "    plans:",
                "    - name: small",
                "      description: small plan",
                "      plan.config:",
                "        provisioning.properties:",
                "          minCores: 1",
                "          minRam: 1",
                "    - name: medium",
                "      description: medium plan",
                "      plan.config:",
                "        provisioning.properties:",
                "          minCores: 2",
                "          minRam: 2",
                "    - name: large",
                "      description: large plan",
                "      plan.config:",
                "        provisioning.properties:",
                "          minCores: 4",
                "          minRam: 4",
                "    plan.config:",
                "      datastore.creation.script.contents: |",
           		"        CREATE USER sqluser WITH PASSWORD '$(string.random)';", 
		        "        CREATE DATABASE mydatabase OWNER sqluser;"
        		);

        List<Plan> plans = strategy.makePlans("test_id", yaml);
        assertEquals(3, plans.size());
        assertEquals("small", plans.get(0).getName());
        assertEquals("small plan", plans.get(0).getDescription());
        assertEquals("medium", plans.get(1).getName());
        assertEquals("medium plan", plans.get(1).getDescription());
        assertEquals("large", plans.get(2).getName());
        assertEquals("large plan", plans.get(2).getDescription());
        Map<String, Object> planMetadata = (Map<String, Object>)plans.get(0).getMetadata();
        assertTrue(planMetadata.containsKey("datastore.creation.script.contents"));
        assertEquals("CREATE USER sqluser WITH PASSWORD 'password';\nCREATE DATABASE mydatabase OWNER sqluser;", 
        		planMetadata.get("datastore.creation.script.contents"));
        assertTrue(planMetadata.containsKey("provisioning.properties"));
        Map<String, Object> provisioningProperties = (Map<String, Object>)planMetadata.get("provisioning.properties");
        assertEquals((Integer)1, Integer.valueOf(provisioningProperties.get("minCores").toString()));
        assertEquals((Integer)1, Integer.valueOf(provisioningProperties.get("minRam").toString()));
    }

}
