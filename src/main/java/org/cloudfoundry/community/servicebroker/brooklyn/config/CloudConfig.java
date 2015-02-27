package org.cloudfoundry.community.servicebroker.brooklyn.config;

import org.springframework.cloud.config.java.ServiceScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Profile("cloud")
@ServiceScan
@Configuration
@ComponentScan(basePackages = "org.cloudfoundry.community.servicebroker")
@EnableMongoRepositories(basePackages = "org.cloudfoundry.community.servicebroker.brooklyn.repository")
public class CloudConfig {

}
