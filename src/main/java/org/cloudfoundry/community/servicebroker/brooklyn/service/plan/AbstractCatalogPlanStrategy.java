package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.brooklyn.rest.domain.CatalogItemSummary;
import org.apache.brooklyn.util.text.NaturalOrderComparator;
import org.cloudfoundry.community.servicebroker.brooklyn.config.BrooklynConfig;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.brooklyn.service.ServiceUtil;
import org.cloudfoundry.community.servicebroker.model.DashboardClient;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

public abstract class AbstractCatalogPlanStrategy implements CatalogPlanStrategy{
	
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
	
	protected PlaceholderReplacer replacer(){
		return replacer;
	}
	
	@Override
	public List<ServiceDefinition> makeServiceDefinitions() {
		Future<List<CatalogItemSummary>> pageFuture = admin.getCatalogApplications(config.includesAllCatalogVersions());
		Map<String, ServiceDefinition> definitions = new HashMap<>();
        Set<String> names = Sets.newHashSet();

        List<CatalogItemSummary> page = ServiceUtil.getFutureValueLoggingError(pageFuture);
        for (CatalogItemSummary app : page) {
			try {
				String id = app.getSymbolicName();
                String name;
                LOG.info("Brooklyn Application={}", app);
                if(config.includesAllCatalogVersions()) {
                    id = ServiceUtil.getUniqueName(id, new HashSet<>(definitions.keySet()));
                    name = ServiceUtil.getSafeName("br_" + app.getName() + "_" + app.getVersion());
                } else {
                    name = ServiceUtil.getUniqueName("br_" + app.getName(), names);
                }
				
				String description = app.getDescription();
				boolean bindable = true;
				boolean planUpdatable = false;
				List<Plan> plans = new ArrayList<>();
				try {
					plans = makePlans(id, app.getPlanYaml());
				} catch(Exception e) {
					LOG.error("unable to make plans: Unexpected blueprint format");
				}
				if (plans.size() == 0) {
					continue;
				}
				List<String> tags = getTags();
				Future<String> iconAsBase64Future = admin.getIconAsBase64(app.getIconUrl());
				String iconUrl = ServiceUtil.getFutureValueLoggingError(iconAsBase64Future);
				Map<String, Object> metadata = getServiceDefinitionMetadata(iconUrl, app.getPlanYaml());
				List<String> requires = getTags();
				DashboardClient dashboardClient = null;
                definitions.put(id, new ServiceDefinition(id, name, description,
                        bindable, planUpdatable, plans, tags, metadata,
                        requires, dashboardClient));
			} catch (Exception e) {
				LOG.error("unable to add catalog item: {}", e.getMessage());
			}
		}
        return new ArrayList<>(definitions.values());
	}
	
    private List<String> getTags() {
		return Arrays.asList();
	}

	private Map<String, Object> getServiceDefinitionMetadata(String iconUrl, String planYaml) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("imageUrl", iconUrl);
        metadata.put("planYaml", planYaml);
		return metadata;
	}

}
