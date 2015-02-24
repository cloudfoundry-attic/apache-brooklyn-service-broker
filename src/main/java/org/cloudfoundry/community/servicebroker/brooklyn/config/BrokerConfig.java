package org.cloudfoundry.community.servicebroker.brooklyn.config;

import org.cloudfoundry.community.servicebroker.model.BrokerApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.config.java.ServiceScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import brooklyn.rest.client.BrooklynApi;

@ServiceScan
@Configuration
@ComponentScan(basePackages = "org.cloudfoundry.community.servicebroker")
@EnableMongoRepositories(basePackages = "org.cloudfoundry.community.servicebroker.brooklyn.repository")
public class BrokerConfig{
	
	@Autowired
	private BrooklynConfig config;


	@Bean
	public BrokerApiVersion brokerApiVersion() {
	    return new BrokerApiVersion();
	}
	
	@Bean
	public BrooklynApi restApi(){
		System.out.printf("connecting to %s with username: %s and password: %s%n",config.toFullUrl(), config.getUsername(), config.getPassword());
		return new BrooklynApi(config.toFullUrl(), config.getUsername(), config.getPassword());
	}
	
}
