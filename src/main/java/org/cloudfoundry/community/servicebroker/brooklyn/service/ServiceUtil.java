package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.Set;
import java.util.concurrent.Future;

import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.text.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceUtil.class);

    public static String getUniqueName(String name, Set<String> names) {
        name = getSafeName(name);
        if (!names.contains(name)) {
            names.add(name);
            return name;
        }
        int i = 1;
        while (names.contains(name + "_" + i)) {
            i++;
        }
        names.add(name + "_" + i);
        return name + "_" + i;
    }

    public static <V> V getFutureValueLoggingError(Future<V> future) {
        V v = null;
        try {
            v = future.get();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            Exceptions.propagateIfFatal(e);
        }
        return v;
    }

    public static String getSafeName(String name) {
        return Strings.makeValidJavaName(name).toLowerCase();
    }
}
