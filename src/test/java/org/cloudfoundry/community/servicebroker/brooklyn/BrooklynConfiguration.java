package org.cloudfoundry.community.servicebroker.brooklyn;

import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrooklynConfiguration {

	public @Bean BrooklynConfig brooklynConfig() {
		return new BrooklynConfig();
	}
}
