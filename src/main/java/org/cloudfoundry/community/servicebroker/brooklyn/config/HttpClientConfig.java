package org.cloudfoundry.community.servicebroker.brooklyn.config;

import javax.net.ssl.SSLContext;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.google.common.collect.ImmutableList;

@Configuration
@Profile("development")
public class HttpClientConfig {
	
	private static final Logger LOG = LoggerFactory.getLogger(HttpClientConfig.class);

	@Autowired
	private BrooklynConfig config;

	@Bean
	public HttpClient httpClient() {
		LOG.info("Using development (trust-self-signed) http client");

		try {
			ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
			SSLContext sslContext = SSLContexts.custom()
					.loadTrustMaterial(null, new TrustSelfSignedStrategy())
					.build();
			LayeredConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
			Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory>create()
			        .register("http", plainsf)
			        .register("https", sslsf)
			        .build();

			HttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(r);

			RequestConfig requestConfig = RequestConfig
					.custom()
					.setSocketTimeout(30000)
					.setConnectTimeout(30000)
					.setTargetPreferredAuthSchemes(ImmutableList.of(AuthSchemes.BASIC))
					.setProxyPreferredAuthSchemes(ImmutableList.of(AuthSchemes.BASIC)).build();

			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(AuthScope.ANY, getUsernamePasswordCredentials());
			
			
			return HttpClients.custom().setConnectionManager(cm)
					.setDefaultCredentialsProvider(credentialsProvider)
					.setDefaultRequestConfig(requestConfig).build();
		} catch (Exception e) {
			throw new RuntimeException("Cannot build HttpClient using self signed certificate");
		}
	}

	private Credentials getUsernamePasswordCredentials() {
		return new UsernamePasswordCredentials(config.getUsername(),
				config.getPassword());
	}
}
