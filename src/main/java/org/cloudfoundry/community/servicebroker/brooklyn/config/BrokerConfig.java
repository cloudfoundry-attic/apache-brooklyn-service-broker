package org.cloudfoundry.community.servicebroker.brooklyn.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.CatalogPlanStrategy;
import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.LocationPlanStrategy;
import org.cloudfoundry.community.servicebroker.model.BrokerApiVersion;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import brooklyn.rest.client.BrooklynApi;

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
	public CatalogPlanStrategy CatalogPlanStrategy(BrooklynRestAdmin admin){
	    return new LocationPlanStrategy(admin);
	}
	

	@Bean
	public BrooklynApi restApi() {
		URL url;
		try {
			url = new URL(config.toFullUrl());
			LOG.info("Creating new brooklynApi for " + url);
			DefaultHttpClient httpClient = new DefaultHttpClient();
			setCredentials(httpClient);
			if (url.getProtocol().equals("https")) {
				LOG.info("Detected https, registering trust all / allow all");
				registerScheme(httpClient, createScheme(url));
			}
			return new BrooklynApi(url, new ApacheHttpClient4Executor(httpClient));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;

	}

	private Scheme createScheme(URL url) {
		Scheme sch = null;
		try {
			sch = new Scheme(url.getProtocol(), url.getPort(),
					new SSLSocketFactory(new TrustAllStrategy(),
							SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER));
		} catch (KeyManagementException | UnrecoverableKeyException
				| NoSuchAlgorithmException | KeyStoreException e) {
			e.printStackTrace();
		}
		return sch;
	}

	private void registerScheme(DefaultHttpClient httpClient, Scheme sch) {
		httpClient.getConnectionManager().getSchemeRegistry().register(sch);
	}

	private void setCredentials(DefaultHttpClient httpClient) {
		httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY,
				getUsernamePasswordCredentials());
	}

	private Credentials getUsernamePasswordCredentials() {
		return new UsernamePasswordCredentials(config.getUsername(),
				config.getPassword());
	}

	public static class TrustAllStrategy implements TrustStrategy {
		@Override
		public boolean isTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			return true;
		}
	}

}
