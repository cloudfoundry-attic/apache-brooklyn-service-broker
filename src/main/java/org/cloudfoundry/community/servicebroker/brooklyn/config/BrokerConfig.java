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
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cloudfoundry.community.servicebroker.model.BrokerApiVersion;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import brooklyn.rest.client.BrooklynApi;

@Configuration
@ComponentScan(basePackages = "org.cloudfoundry.community.servicebroker")
public class BrokerConfig {

	@Autowired
	private BrooklynConfig config;

	@Bean
	public BrokerApiVersion brokerApiVersion() {
		return new BrokerApiVersion();
	}

	@Bean
	public BrooklynApi restApi() {
		// System.out.printf("connecting to %s with username: %s and password: %s%n",config.toFullUrl(),
		// config.getUsername(), config.getPassword());
		// BrooklynApi brooklynApi = new BrooklynApi(config.toFullUrl(),
		// config.getUsername(), config.getPassword());
		
		

		URL url;
		try {
			url = new URL(config.toFullUrl());
			DefaultHttpClient httpClient = new DefaultHttpClient();
			httpClient.getCredentialsProvider().setCredentials(
					AuthScope.ANY, new UsernamePasswordCredentials(config.getUsername(), config.getPassword()));
			if (url.getProtocol().equals("https")) {
				Scheme sch = new Scheme(url.getProtocol(), url.getPort(), 
						new SSLSocketFactory(new TrustAllStrategy(), SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER));
				httpClient.getConnectionManager().getSchemeRegistry().register(sch);
			}
			BrooklynApi brooklynApi = new BrooklynApi(url,
					new ApacheHttpClient4Executor(httpClient));
			return brooklynApi;
		} catch (MalformedURLException 
				| KeyManagementException 
				| UnrecoverableKeyException 
				| NoSuchAlgorithmException 
				| KeyStoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static class TrustAllStrategy implements TrustStrategy {
		@Override
		public boolean isTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			return true;
		}
	}

}
