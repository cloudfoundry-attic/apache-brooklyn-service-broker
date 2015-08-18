package org.cloudfoundry.community.servicebroker.brooklyn.config;

import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.CatalogPlanStrategy;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.PlaceholderReplacer;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.SizePlanStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("size-plan")
public class SizePlanConfig {
    
    @Bean
    public CatalogPlanStrategy planStrategy(BrooklynRestAdmin brooklynRestAdmin, BrooklynConfig brooklynConfig, PlaceholderReplacer replacer){
        return new SizePlanStrategy(brooklynRestAdmin, brooklynConfig, replacer);
    }

}
