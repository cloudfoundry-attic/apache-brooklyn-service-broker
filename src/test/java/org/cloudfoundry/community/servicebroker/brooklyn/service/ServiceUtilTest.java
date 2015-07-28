package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Sets;

public class ServiceUtilTest {

    @Test
    public void testGetUniqueName() {
        Set<String> names = Sets.newHashSet();
        String name1 = ServiceUtil.getUniqueName("foo", names);
        Assert.assertEquals(name1, "foo");
        String name2 = ServiceUtil.getUniqueName("foo", names);
        Assert.assertEquals(name2, "foo_1");
        String name3 = ServiceUtil.getUniqueName("foo", names);
        Assert.assertEquals(name3, "foo_2");
    }

}
