package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.springframework.beans.factory.annotation.Autowired;

import brooklyn.util.yaml.Yamls;

import com.google.common.collect.Maps;

public class SizePlanStrategy implements CatalogPlanStrategy {

    private final BrooklynConfig brooklynConfig;

    @Autowired
    public SizePlanStrategy(BrooklynConfig brooklynConfig) {
        this.brooklynConfig = brooklynConfig;
    }

    @Override
    public List<Plan> makePlans(String serviceId, String yaml) {
        List<Plan> plans = new ArrayList<>();
        if (yaml == null) {
            return plans;
        }

        Iterator<Object> iterator = Yamls.parseAll(yaml).iterator();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            if (next instanceof Map){
                Map<String, Object> map = (Map<String, Object>) next;
                if (map.containsKey("brooklyn.config")){
                    Map<String, Object> brooklynConfig = (Map<String, Object>)map.get("brooklyn.config");
                    return parseConfig(brooklynConfig, serviceId);
                }
            }
        }
        return plans;
    }

    private List<Plan> parseConfig(Map<String, Object> brooklynConfigMap, String serviceId) {
        List<Plan> plans = new ArrayList<>();
        Map<String, Object> brokerConfig = (Map<String, Object>)brooklynConfigMap.get("broker.config");
        Map<String, Object> planDefinitions = (Map<String, Object>)brokerConfig.get("plans");
        for (String planName : planDefinitions.keySet()) {
            Map<String, Object> planDefinition = (Map<String, Object>) planDefinitions.get(planName);
            Map<String, Object> properties = Maps.newHashMap();
            properties.put("provisioning.properties", planDefinition.get("provisioning.properties"));
            properties.put("location", brooklynConfig.getLocation());
            String id = serviceId + "." + planName;
            String name = planName;
            String description = planName;
            Plan plan = new Plan(id, name, description, properties);
            plans.add(plan);
        }
        return plans;
    }

}
