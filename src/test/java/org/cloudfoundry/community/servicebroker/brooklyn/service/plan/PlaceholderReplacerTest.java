package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.collections.MutableMap;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class PlaceholderReplacerTest {

    private static String PLACEHOLDER = "$(string.random)";

    private PlaceholderReplacer placeholderReplacer;

    @Before
    public void setup() {
        placeholderReplacer = new PlaceholderReplacer(new Random());
    }

    @Test
    public void testRandomString() {
        final ImmutableList<Integer> lengths = ImmutableList.of(4, 8, 15, 25, 34, 345, 255, 123);

        for (Integer length : lengths) {
            final String randomString = placeholderReplacer.randomString(length);
            assertEquals((int) length, randomString.length());
        }
    }

    @Test
    public void testReplaceValueString() {
        final MutableList<String> fixtures = MutableList.of("foo", "bar", "${string.random}", PLACEHOLDER);

        for (String fixture : fixtures) {
            final String actualValue = placeholderReplacer.replaceValue(fixture);
            if (PLACEHOLDER.equals(fixture)) {
                assertNotEquals(fixture, actualValue);
            } else {
                assertEquals(fixture, actualValue);
            }
            assertEquals(PLACEHOLDER.equals(fixture) ? 8 : fixture.length(), actualValue.length());
        }
    }

    @Test
    public void testReplaceValueList() {
        final ImmutableList<Object> fixtures = ImmutableList.of(
                "foo",
                PLACEHOLDER,
                new Date(),
                ImmutableList.of("bar", PLACEHOLDER),
                ImmutableMap.of("foo", "bar", PLACEHOLDER, PLACEHOLDER));

        final List<Object> actualValues = placeholderReplacer.replaceValues(MutableList.copyOf(fixtures));

        this.assertReplacedValues(fixtures, actualValues);
    }

    @Test
    public void testReplaceValueMap() {
        final ImmutableMap<String, Object> fixtures = ImmutableMap.of(
                "foo", "bar",
                PLACEHOLDER, PLACEHOLDER,
                "object", new Date(),
                "map", ImmutableMap.of(
                        "foo", "bar",
                        PLACEHOLDER, PLACEHOLDER
                ),
                "list", ImmutableList.of("bar", PLACEHOLDER)
        );

        final Map<String, Object> actualValues = placeholderReplacer.replaceValues(MutableMap.copyOf(fixtures));

        this.assertReplacedValues(fixtures, actualValues);
    }

    private void assertReplacedValues(Object fixture, Object actual) {
        if (fixture instanceof Map) {
            assertTrue(actual instanceof Map);
            assertEquals(((Map) fixture).size(), ((Map) actual).size());
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) fixture).entrySet()) {
                assertTrue(((Map) actual).containsKey(entry.getKey()));
                this.assertReplacedValues(entry.getValue(), ((Map) actual).get(entry.getKey()));
            }

        } else if (fixture instanceof List) {
            assertTrue(actual instanceof List);
            assertEquals(((List) fixture).size(), ((List) actual).size());
            for (int i = 0; i < ((List) fixture).size(); i++) {
                this.assertReplacedValues(((List) fixture).get(i), ((List) actual).get(i));
            }
        } else if (fixture instanceof String) {
            assertTrue(actual instanceof String);
            if (PLACEHOLDER.equals(fixture)) {
                assertNotEquals(fixture, actual);
            } else {
                assertEquals(fixture, actual);
            }
        } else {
            assertEquals(fixture, actual);
        }
    }

    @Test
    public void testReplaceValueInListOfList() {
        final MutableList<Object> fixtures = MutableList.of(
                MutableList.of("foo", "bar", PLACEHOLDER));

        final List<Object> replacedValues = placeholderReplacer.replaceValues(fixtures);

        assertNotEquals(replacedValues, fixtures);
        assertEquals(replacedValues.size(), fixtures.size());
        boolean found = false;
        for (Object replacedValue : replacedValues) {
            for (Object s : ((List<Object>) replacedValue)) {
                assertTrue(s instanceof String);
                found |= ((String) s).equals(PLACEHOLDER);
            }
        }
        assertFalse(found);
    }
}
