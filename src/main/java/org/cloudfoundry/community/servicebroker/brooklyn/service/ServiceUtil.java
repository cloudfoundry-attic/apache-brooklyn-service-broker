package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.text.Strings;

public class ServiceUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceUtil.class);

    public static String getUniqueName(String name, Set<String> names) {
        name = Strings.makeValidJavaName(name).toLowerCase();
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
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            Exceptions.propagateIfFatal(e);
        }
        return v;
    }
}
