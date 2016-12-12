package org.cloudfoundry.community.servicebroker.brooklyn.model;

import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
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
		String brooklynCatalogId = "my-service";
		String location = "localhost";
		Map<String,Object> config = ImmutableMap.of("cluster.minSize", 4);

		DefaultBlueprintPlan plan = new DefaultBlueprintPlan("testId", "testName", "testDescription", "Test App", config);
		ObjectWriter writer = new ObjectMapper().writer();
		String configJson = writer.writeValueAsString(config);
		when(request.getParameters()).thenReturn((Map)config);
		String expected = String.format("{\"name\":\"Test App (CF Service)\", \"services\":[{\"type\": \"%s\", \"id\": \"broker.entity\"}], \"locations\": [\"%s\"], \"brooklyn.config\":%s}",
				brooklynCatalogId, location, configJson);
		String result = plan.toBlueprint(brooklynCatalogId, location, request);
		assertEquals(expected, result);
	}
	
	@Test
	public void testToBlueprintWithoutConfig() throws JsonProcessingException{
		String location = "localhost";
		String brooklynCatalogId = "my-service";
		Map<String,Object> config = ImmutableMap.of();
		DefaultBlueprintPlan plan = new DefaultBlueprintPlan("testId", "testName", "testDescription", "Test App", config);
		when(request.getParameters()).thenReturn((Map)config);
		String expected = String.format("{\"name\":\"Test App (CF Service)\", \"services\":[{\"type\": \"%s\", \"id\": \"broker.entity\"}], \"locations\": [\"%s\"]}",
				brooklynCatalogId, location);
		String result = plan.toBlueprint(brooklynCatalogId, location, request);
		assertEquals(expected, result);
	}
	
	@Test
	public void testToBlueprintWithNullConfig() throws JsonProcessingException{
		String location = "localhost";
		String brooklynCatalogId = "my-service";
		Map<String,Object> config = null;
		DefaultBlueprintPlan plan = new DefaultBlueprintPlan("testId", "testName", "testDescription", "Test App", config);
		when(request.getParameters()).thenReturn((Map)config);
		String expected = String.format("{\"name\":\"Test App (CF Service)\", \"services\":[{\"type\": \"%s\", \"id\": \"broker.entity\"}], \"locations\": [\"%s\"]}",
				brooklynCatalogId, location);
		String result = plan.toBlueprint(brooklynCatalogId, location, request);
		assertEquals(expected, result);
	}

	@Test
	public void testToBlueprintWithLocation() throws JsonProcessingException {
		Map<String, Object> metadata = ImmutableMap.of("location", "AWS California");
		String brooklynCatalogId = "my-service";
		DefaultBlueprintPlan plan = new DefaultBlueprintPlan("testId", "testName", "testDescription", "Test App", metadata);
		String result = plan.toBlueprint(brooklynCatalogId, null, request);
		String expected = String.format("{\"name\":\"Test App (CF Service)\", \"services\":[{\"type\": \"%s\", \"id\": \"broker.entity\"}], \"locations\": [\"%s\"]}",
				brooklynCatalogId, "AWS California");
		
		assertEquals(expected, result);
		
		metadata = ImmutableMap.of("location", ImmutableMap.of(
				"jclouds:aws-ec2", ImmutableMap.of("identity", "***", "credential", "***", "region", "ap-southeast-1")
		));
		plan = new DefaultBlueprintPlan("testId", "testName", "testDescription", "Test App", metadata);
		
		ObjectWriter writer = new ObjectMapper().writer();
		String location = writer.writeValueAsString(metadata.get("location"));
		expected = String.format("{\"name\":\"Test App (CF Service)\", \"services\":[{\"type\": \"%s\", \"id\": \"broker.entity\"}], \"locations\": [%s]}",
				brooklynCatalogId, location);
		result = plan.toBlueprint(brooklynCatalogId, null, request);
		
		assertEquals(expected, result);
	}

}
