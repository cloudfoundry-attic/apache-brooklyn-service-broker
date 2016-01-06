package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.brooklyn.rest.domain.LocationSummary;
import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.cloudfoundry.community.servicebroker.brooklyn.model.DefaultBlueprintPlan;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.brooklyn.service.ServiceUtil;
import org.springframework.cloud.servicebroker.model.Plan;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

public class LocationPlanStrategy extends AbstractCatalogPlanStrategy{

    @Autowired
    public LocationPlanStrategy(BrooklynRestAdmin admin, PlaceholderReplacer replacer, BrooklynConfig config) {
        super(admin, replacer, config);
    }

    @Override
    public List<Plan> makePlans(String serviceId, String appName, Object rootElement) {
        List<Plan> plans = new ArrayList<>();
        // check if yaml contains a location
        // if it does extract that and use it
        // as the plan.
        if (rootElement instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) rootElement;
            if (map.containsKey("location")) {
                Object location = map.get("location");
                if (location instanceof Map) {
                    // just use the keys
                    for (String s : ((Map<String, Object>) location).keySet()) {
                        String id = serviceId + "." + s;
                        String name = s;
                        String description = "Deploys to " + s;
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("location", location);
                        plans.add(new DefaultBlueprintPlan(id, name, description, appName, metadata));
                    }
                } else if (location instanceof String) {
                    String id = serviceId + "." + location;
                    String name = (String) location;
                    String description = "Deploys to " + location;
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("location", name);
                    plans.add(new DefaultBlueprintPlan(id, name, description, appName, metadata));
                }
                return plans;
            }
        }

        Future<List<LocationSummary>> locationsFuture = getAdmin().getLocations();
        List<LocationSummary> locations = ServiceUtil.getFutureValueLoggingError(locationsFuture);

        Set<String> names = Sets.newHashSet();

        for (LocationSummary l : locations) {
            String id = serviceId + "." + l.getName();
            String name = ServiceUtil.getUniqueName(l.getName(), names);
            String description = "Deploys to " + l.getName();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("location", l.getName());
            plans.add(new DefaultBlueprintPlan(id, name, description, appName, metadata));
        }
        return plans;
    }
}
