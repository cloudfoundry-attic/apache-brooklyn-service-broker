package org.cloudfoundry.community.servicebroker.brooklyn.config;

import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.CatalogPlanStrategy;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.UserDefinedPlanStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("user-defined-plan")
public class UserDefinedPlanConfig {

	@Bean
    public CatalogPlanStrategy planStrategy(BrooklynConfig brooklynConfig){
        return new UserDefinedPlanStrategy();
    }
}
