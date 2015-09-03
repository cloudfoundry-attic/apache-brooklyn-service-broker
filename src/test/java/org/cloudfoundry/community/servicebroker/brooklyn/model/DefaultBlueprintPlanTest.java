package org.cloudfoundry.community.servicebroker.brooklyn.model;

import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableMap;

public class DefaultBlueprintPlanTest {
	
	@Mock
	private CreateServiceInstanceRequest request;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testToBlueprintWithConfig() throws JsonProcessingException{
		String location = "localhost";
		Map<String,Object> config = ImmutableMap.of("cluster.minSize", 4);

		DefaultBlueprintPlan plan = new DefaultBlueprintPlan("testId", "testName", "testDescription", config);
		ObjectWriter writer = new ObjectMapper().writer();
		String configJson = writer.writeValueAsString(config);
		when(request.getParameters()).thenReturn((Map)config);
		String expected = String.format("{\"services\":[{\"type\": \"%s\"}], \"locations\": [\"%s\"], \"brooklyn.config\":%s}",
				request.getServiceDefinitionId(), location, configJson);
		String result = plan.toBlueprint(location, request);
		assertEquals(expected, result);
	}
	
	@Test
	public void testToBlueprintWithoutConfig() throws JsonProcessingException{
		String location = "localhost";
		Map<String,Object> config = ImmutableMap.of();
		DefaultBlueprintPlan plan = new DefaultBlueprintPlan("testId", "testName", "testDescription", config);
		when(request.getParameters()).thenReturn((Map)config);
		String expected = String.format("{\"services\":[{\"type\": \"%s\"}], \"locations\": [\"%s\"]}",
				request.getServiceDefinitionId(), location);
		String result = plan.toBlueprint(location, request);
		assertEquals(expected, result);
	}
	
	@Test
	public void testToBlueprintWithNullConfig() throws JsonProcessingException{
		String location = "localhost";
		Map<String,Object> config = null;
		DefaultBlueprintPlan plan = new DefaultBlueprintPlan("testId", "testName", "testDescription", config);
		when(request.getParameters()).thenReturn((Map)config);
		String expected = String.format("{\"services\":[{\"type\": \"%s\"}], \"locations\": [\"%s\"]}",
				request.getServiceDefinitionId(), location);
		String result = plan.toBlueprint(location, request);
		assertEquals(expected, result);
	}

}
