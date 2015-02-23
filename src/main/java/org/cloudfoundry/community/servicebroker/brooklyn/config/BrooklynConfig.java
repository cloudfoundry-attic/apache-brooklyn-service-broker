package org.cloudfoundry.community.servicebroker.brooklyn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix="brooklyn")
public class BrooklynConfig {

	private String uri;
	private String username;
	private String password;
	
	public String getUri() {
		return uri;
	}
	
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public String toFullUrl(String... resource){
		StringBuilder sb = new StringBuilder();
		sb.append(getUri());
		for(String s : resource){
			sb.append("/");
			sb.append(s);
		}
		return sb.toString();
	}

	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}

	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
}
