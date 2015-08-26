package org.cloudfoundry.community.servicebroker.brooklyn;

import java.util.Random;

import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.PlaceholderReplacer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrooklynConfiguration {

	public @Bean BrooklynConfig brooklynConfig() {
		return new BrooklynConfig();
	}

    public @Bean
    PlaceholderReplacer placeholderReplacer(){
        return new PlaceholderReplacer(new Random(0));
    }

}
