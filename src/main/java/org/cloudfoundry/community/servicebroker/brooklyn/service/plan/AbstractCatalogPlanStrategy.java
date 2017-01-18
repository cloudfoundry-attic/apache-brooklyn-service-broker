package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.brooklyn.rest.domain.CatalogItemSummary;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.guava.Maybe;
import org.apache.brooklyn.util.text.NaturalOrderComparator;
import org.apache.brooklyn.util.text.Strings;
import org.apache.brooklyn.util.yaml.Yamls;
import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.cloudfoundry.community.servicebroker.brooklyn.model.UserDefinedBlueprintPlan;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.brooklyn.service.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.model.Plan;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class AbstractCatalogPlanStrategy implements CatalogPlanStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCatalogPlanStrategy.class);

    private BrooklynRestAdmin admin;
    private PlaceholderReplacer replacer;
    private BrooklynConfig config;

    @Autowired
    public AbstractCatalogPlanStrategy(BrooklynRestAdmin admin, PlaceholderReplacer replacer, BrooklynConfig config) {
        this.admin = admin;
        this.replacer = replacer;
        this.config = config;
    }

    protected BrooklynRestAdmin getAdmin() {
        return admin;
    }

    protected PlaceholderReplacer replacer() {
        return replacer;
    }

    @Override
    public List<ServiceDefinition> makeServiceDefinitions() {
        Future<List<CatalogItemSummary>> pageFuture = admin.getCatalogApplications(config.includesAllCatalogVersions());
        Map<String, ServiceDefinition> definitions = new HashMap<>();

        Set<String> ids = Sets.newHashSet();
        Set<String> names = Sets.newHashSet();
        String namespace = config.getNamespace() == null ? "br" : config.getNamespace();
        String userDefinedId = makeId(namespace, "ApacheBrooklynBlueprint", ids);
        String userDefinedName = makeName(namespace, "Apache Brooklyn Blueprint", "1.0", names);
        UserDefinedBlueprintPlan userDefinedPlan = new UserDefinedBlueprintPlan(userDefinedId, "default", "Users specify a complete Apache Brooklyn blueprint passed in as parameters", ImmutableMap.of());
        definitions.put(userDefinedId, new ServiceDefinition(userDefinedId,
                userDefinedName,
                "Users to specify a complete Apache Brooklyn blueprint",
                true,
                false,
                ImmutableList.of(userDefinedPlan),
                ImmutableList.of(),
                ImmutableMap.of("brooklynCatalogId", "no catalog id"),
                ImmutableList.of(),
                null)
        );
        List<CatalogItemSummary> page = ServiceUtil.getFutureValueLoggingError(pageFuture);
        Collections.sort(page, (o1, o2) -> NaturalOrderComparator.INSTANCE.compare(o1.getVersion(), o2.getVersion()));
        for (CatalogItemSummary app : page) {
            try {
                Object rootElement = getRootElement(app.getPlanYaml());
                if (isHidden(rootElement)) {
                    continue;
                }

                String id = makeId(namespace, app.getSymbolicName(), ids);
                String name = makeName(namespace, app.getName(), app.getVersion(), names);

                LOG.info("name={}", name);
                List<Plan> plans = makePlans(id, app.getName(), rootElement);
                if (plans.isEmpty()) {
                    continue;
                }

                Map<String, Object> metadata = Maps.newLinkedHashMap();
                Maybe<Map<String, Object>> brokerConfig = getBrokerConfig(rootElement);
                if(brokerConfig.isPresent()){
                    Map<String, Object> brokerConfingMap = brokerConfig.get();
                    if(brokerConfingMap.containsKey("metadata")) {
                        metadata.putAll((Map<String, Object>)brokerConfingMap.get("metadata"));
                    }
                }
                metadata.putAll(getServiceDefinitionMetadata(app.getId(), app.getIconUrl(), app.getPlanYaml()));
                definitions.put(id, new ServiceDefinition(
                        id, name, app.getDescription(),
                        true, // bindable
                        false, // planUpdatable
                        plans, getTags(), metadata, getRequires(),
                        null //dashboardClient
                ));
            } catch (Exception e) {
                LOG.error("unable to add catalog item: {}", e.getMessage());
            }
        }
        return new ArrayList<>(definitions.values());
    }

    private Object getRootElement(String planYaml) {
        try {
            return Iterables.getOnlyElement(Yamls.parseAll(planYaml));
        } catch (Exception e) {
            LOG.error("unable to parse YAML");
            throw Exceptions.propagate(e);
        }
    }

    private String makeId(String namespace, String symbolicName, Set<String> ids) {
        String id = namespace + "_" + symbolicName;
        if (config.includesAllCatalogVersions()) {
            id = ServiceUtil.getUniqueName(id, ids);
        }
        return id;
    }

    private String makeName(String namespace, String name, String version, Set<String> names) {
        return config.includesAllCatalogVersions()
                ? ServiceUtil.getSafeName(namespace + "_" + name + "_" + version)
                : ServiceUtil.getUniqueName(namespace + "_" + name, names);
    }

    private boolean isHidden(Object rootElement) {
        Maybe<Map<String, Object>> maybeBrokerConfig = getBrokerConfig(rootElement);
        if (maybeBrokerConfig.isAbsent()) {
            return false;
        }
        return Boolean.TRUE.equals(maybeBrokerConfig.get().get("hidden"));
    }

    protected Maybe<Map<String, Object>> getBrokerConfig(Object rootElement) {
        if (rootElement == null) {
            return Maybe.absent();
        }
        if (rootElement instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) rootElement;
            if (map.containsKey("brooklyn.config")) {
                Map<String, Object> brooklynConfig = (Map<String, Object>) map.get("brooklyn.config");
                if (brooklynConfig != null && brooklynConfig.containsKey("broker.config")) {
                    Map<String, Object> brokerConfig = (Map<String, Object>) brooklynConfig.get("broker.config");
                    if (brokerConfig != null) {
                        return Maybe.of(brokerConfig);
                    }
                }
            }
        }
        return Maybe.absent();
    }

    private List<String> getTags() {
        return Arrays.asList();
    }

    private List<String> getRequires() {
        return Arrays.asList();
    }

    private Map<String, Object> getServiceDefinitionMetadata(String brooklynCatalogId, String iconUrl, String planYaml) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("brooklynCatalogId", brooklynCatalogId);
        if (!Strings.isEmpty(iconUrl)) {
            Future<String> iconAsBase64Future = admin.getIconAsBase64(iconUrl);
            iconUrl = ServiceUtil.getFutureValueLoggingError(iconAsBase64Future);
            metadata.put("imageUrl", iconUrl);
        }
        metadata.put("planYaml", planYaml);
        return metadata;
    }

}
