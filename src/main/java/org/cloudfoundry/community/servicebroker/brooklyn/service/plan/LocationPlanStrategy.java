package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.brooklyn.service.ServiceUtil;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.springframework.beans.factory.annotation.Autowired;

import brooklyn.rest.domain.LocationSummary;
import brooklyn.util.yaml.Yamls;

import com.google.common.collect.Sets;

public class LocationPlanStrategy implements CatalogPlanStrategy{


    private BrooklynRestAdmin admin;

    @Autowired
    public LocationPlanStrategy(BrooklynRestAdmin admin) {
        this.admin = admin;
    }

    @Override
    public List<Plan> makePlans(String serviceId, String yaml) {
        List<Plan> plans = new ArrayList<>();
        // check if yaml contains a location
        // if it does extract that and use it
        // as the plan.
        if (yaml != null) {
            Iterator<Object> iterator = Yamls.parseAll(yaml).iterator();
            while (iterator.hasNext()) {
                Object next = iterator.next();
                if (next instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) next;
                    if (map.containsKey("location")) {
                        Object location = map.get("location");
                        if (location instanceof Map) {
                            // just use the keys
                            for (String s : ((Map<String, Object>) location).keySet()) {
                                String id = serviceId + "." + s;
                                String name = s;
                                String description = "The location on which to deploy this service";
                                Map<String, Object> metadata = new HashMap<>();
                                metadata.put("location", s);
                                plans.add(new Plan(id, name, description, metadata));
                            }
                        } else if (location instanceof String) {
                            String id = serviceId + "." + location;
                            String name = (String) location;
                            String description = "The location on which to deploy this service";
                            Map<String, Object> metadata = new HashMap<>();
                            metadata.put("location", name);
                            plans.add(new Plan(id, name, description, metadata));
                        }
                        return plans;
                    }
                }
            }
        }

        Future<List<LocationSummary>> locationsFuture = admin.getLocations();
        List<LocationSummary> locations = ServiceUtil.getFutureValueLoggingError(locationsFuture);

        Set<String> names = Sets.newHashSet();

        for (LocationSummary l : locations) {
            String id = serviceId + "." + l.getName();
            String name = ServiceUtil.getUniqueName(l.getName(), names);
            String description = "The location on which to deploy this service";
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("location", name);
            plans.add(new Plan(id, name, description, metadata));
        }
        return plans;
    }

}
