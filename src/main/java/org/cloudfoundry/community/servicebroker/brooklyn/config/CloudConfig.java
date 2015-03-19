package org.cloudfoundry.community.servicebroker.brooklyn.config;

import org.springframework.cloud.config.java.ServiceScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("cloud")
@ServiceScan
@Configuration
@ComponentScan(basePackages = "org.cloudfoundry.community.servicebroker")
public class CloudConfig {

}
