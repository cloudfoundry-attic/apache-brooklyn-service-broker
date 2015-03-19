package org.cloudfoundry.community.servicebroker.brooklyn.config;

import org.cloudfoundry.community.servicebroker.brooklyn.repository.Respositories;
import org.cloudfoundry.community.servicebroker.model.BrokerApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import brooklyn.rest.client.BrooklynApi;

@Configuration
@ComponentScan(basePackages = "org.cloudfoundry.community.servicebroker")
public class BrokerConfig{
	
	@Autowired
	private BrooklynConfig config;


	@Bean
	public BrokerApiVersion brokerApiVersion() {
	    return new BrokerApiVersion();
	}
	
	
	@Bean
	public BrooklynApi restApi(){
		//System.out.printf("connecting to %s with username: %s and password: %s%n",config.toFullUrl(), config.getUsername(), config.getPassword());
		BrooklynApi brooklynApi = new BrooklynApi(config.toFullUrl(), config.getUsername(), config.getPassword());
		Respositories.createRepositories(brooklynApi);
		return brooklynApi;
	}
	
}
