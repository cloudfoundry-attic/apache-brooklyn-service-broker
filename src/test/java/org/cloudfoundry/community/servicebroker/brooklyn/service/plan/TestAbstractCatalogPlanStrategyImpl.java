package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import java.util.List;

import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableList;

public class TestAbstractCatalogPlanStrategyImpl extends AbstractCatalogPlanStrategy {


    private static final List<Plan> CATALOG_PLANS = ImmutableList.of(
            new Plan("test_plan", "test_plan", "A test plan")
    );

    @Autowired
    public TestAbstractCatalogPlanStrategyImpl(BrooklynRestAdmin admin, PlaceholderReplacer replacer, BrooklynConfig config) {
        super(admin, replacer, config);
    }

    @Override
    public List<Plan> makePlans(String serviceId, String yaml) {
        return CATALOG_PLANS;
    }
}
