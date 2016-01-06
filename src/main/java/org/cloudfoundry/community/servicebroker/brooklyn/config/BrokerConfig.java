package org.cloudfoundry.community.servicebroker.brooklyn.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import org.apache.brooklyn.rest.client.BrooklynApi;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.CatalogPlanStrategy;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.LocationPlanStrategy;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.PlaceholderReplacer;
import org.springframework.cloud.servicebroker.model.BrokerApiVersion;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableList;

@Configuration
@ComponentScan(basePackages = "org.cloudfoundry.community.servicebroker")
public class BrokerConfig {

	private static final Logger LOG = LoggerFactory.getLogger(BrokerConfig.class);

	@Autowired
	private BrooklynConfig config;

	@Bean
	public BrokerApiVersion brokerApiVersion() {
		return new BrokerApiVersion();
	}

	@Bean
	@ConditionalOnMissingBean(CatalogPlanStrategy.class)
	public CatalogPlanStrategy CatalogPlanStrategy(BrooklynRestAdmin admin, PlaceholderReplacer replacer, BrooklynConfig config) {
	    return new LocationPlanStrategy(admin, replacer, config);
	}
	
	@Bean
	public PlaceholderReplacer placeholderReplacer(){
		return new PlaceholderReplacer(new Random());
	}
	
	@Bean
	@ConditionalOnMissingBean(HttpClient.class)
	public HttpClient httpClient(){
		HttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(30000)
                .setConnectTimeout(30000)
                .setTargetPreferredAuthSchemes(ImmutableList.of(AuthSchemes.BASIC))
                .setProxyPreferredAuthSchemes(ImmutableList.of(AuthSchemes.BASIC))
                .build();

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, getUsernamePasswordCredentials());
        return HttpClients.custom().setConnectionManager(cm)
                .setDefaultCredentialsProvider(credentialsProvider)
                .setDefaultRequestConfig(requestConfig)
                .build();
	}


	@Bean
	public BrooklynApi restApi(HttpClient httpClient) {
		URL url;
		try {
			url = new URL(config.toFullUrl());
			LOG.info("Creating new brooklynApi for " + url);
            
			return new BrooklynApi(url, new ApacheHttpClient4Executor(httpClient));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;

	}

	private Credentials getUsernamePasswordCredentials() {
		return new UsernamePasswordCredentials(config.getUsername(),
				config.getPassword());
	}

}
