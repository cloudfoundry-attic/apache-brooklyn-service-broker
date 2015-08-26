package org.cloudfoundry.community.servicebroker.brooklyn.config;

import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.CatalogPlanStrategy;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.LocationPlanStrategy;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.PlaceholderReplacer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("location-plan")
public class LocationPlanConfig {

    @Bean
    public CatalogPlanStrategy planStrategy(BrooklynRestAdmin brooklynRestAdmin, PlaceholderReplacer replacer, BrooklynConfig brooklynConfig) {
        return new LocationPlanStrategy(brooklynRestAdmin, replacer, brooklynConfig);
    }
}
