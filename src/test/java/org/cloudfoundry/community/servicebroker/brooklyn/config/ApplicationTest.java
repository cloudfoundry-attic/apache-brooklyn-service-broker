package org.cloudfoundry.community.servicebroker.brooklyn.config;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.builder.SpringApplicationBuilder;

public class ApplicationTest {

    @Mock
    private SpringApplicationBuilder builder;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConfigureAddsApplicationClassAsSource() {
        when(builder.sources(any(Class.class))).thenReturn(builder);

        new Application().configure(builder);

        verify(builder).sources(Application.class);
    }
}
