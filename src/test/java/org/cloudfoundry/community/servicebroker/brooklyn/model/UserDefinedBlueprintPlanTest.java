package org.cloudfoundry.community.servicebroker.brooklyn.model;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class UserDefinedBlueprintPlanTest {
	
	@Mock
	private CreateServiceInstanceRequest request;
	
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }
	
	@Test
	public void testToBlueprint() throws JsonProcessingException{
		UserDefinedBlueprintPlan plan = new UserDefinedBlueprintPlan("testId", "testName", "testDescription", ImmutableMap.of());
		Map<?, ?> map = ImmutableMap.<Object,Object>of(
				"name", "testUserDefinedService",
				"services", ImmutableList.of(
						ImmutableMap.of("type", "development.test.MyTestService")
				));
		when(request.getParameters()).thenReturn((Map)map);
		String result = plan.toBlueprint(null, "anyLocation", request);
		ObjectMapper om = new ObjectMapper();
		String expected = om.writeValueAsString(map);
		assertEquals(expected, result);
	}

}
