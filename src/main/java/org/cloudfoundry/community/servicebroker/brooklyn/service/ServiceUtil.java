package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.Set;

import brooklyn.util.text.Strings;

public class ServiceUtil {

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
}
