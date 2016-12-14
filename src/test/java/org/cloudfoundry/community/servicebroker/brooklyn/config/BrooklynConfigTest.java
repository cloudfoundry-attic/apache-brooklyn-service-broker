package org.cloudfoundry.community.servicebroker.brooklyn.config;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;


public class BrooklynConfigTest {

    private BrooklynConfig brooklynConfig = new BrooklynConfig();

    @Test
    public void testToFullUrl() {
        final ImmutableMap<String, ImmutableMap<String, String[]>> fixtures = ImmutableMap.of(
                "foo", ImmutableMap.of("foo", new String[]{}),
                "/bar", ImmutableMap.of("", new String[]{"bar"}),
                "foo/bar", ImmutableMap.of("foo", new String[]{"bar"}),
                "foo/bar/zoo", ImmutableMap.of("foo", new String[]{"bar", "zoo"}));

        for (Map.Entry<String, ImmutableMap<String, String[]>> fixture : fixtures.entrySet()) {
            final Map.Entry<String, String[]> next = fixture.getValue().entrySet().iterator().next();
            brooklynConfig.setUri(next.getKey());

            assertEquals(fixture.getKey(), brooklynConfig.toFullUrl(next.getValue()));
        }
    }

    @Test
    public void testGetLocationReturnsLocalHostIfLocationIsBlank() {
        final String[] locations = {null, ""};

        for (String location : locations) {
            brooklynConfig.setLocation(location);
            assertEquals("localhost", brooklynConfig.getLocation());
        }
    }

    @Test
    public void testGetLocationReturnsLocationIfLocationNotBlank() {
        String location = "foo";

        brooklynConfig.setLocation(location);

        assertEquals(location, brooklynConfig.getLocation());
    }
}
