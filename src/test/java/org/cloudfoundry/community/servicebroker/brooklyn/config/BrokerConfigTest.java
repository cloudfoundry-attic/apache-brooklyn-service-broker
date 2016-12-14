package org.cloudfoundry.community.servicebroker.brooklyn.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import org.apache.brooklyn.rest.client.BrooklynApi;
import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BrokerConfigTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private BrooklynConfig brooklynConfig;

    @InjectMocks
    private BrokerConfig brokerConfig = new BrokerConfig();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRestApiReturnsNullIfURLIsMalformed() {
        when(brooklynConfig.toFullUrl()).thenReturn("invalidUrl");

        final BrooklynApi brooklynApi = brokerConfig.restApi(httpClient);

        assertNull(brooklynApi);
    }

    @Test
    public void testRestApiReturnsBrooklynIfURLIsFine() {
        when(brooklynConfig.toFullUrl()).thenReturn("http://brooklyn.apache.org");

        final BrooklynApi brooklynApi = brokerConfig.restApi(httpClient);

        assertNotNull(brooklynApi);
    }
}
