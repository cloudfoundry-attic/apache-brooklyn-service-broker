package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.brooklyn.rest.domain.CatalogItemSummary;
import org.apache.brooklyn.util.text.NaturalOrderComparator;
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
	
	@Autowired
	public AbstractCatalogPlanStrategy(BrooklynRestAdmin admin, PlaceholderReplacer replacer) {
		this.admin = admin;
		this.replacer = replacer;
	}
	
	protected BrooklynRestAdmin getAdmin() {
		return admin;
	}
	
	protected PlaceholderReplacer replacer(){
		return replacer;
	}
	
	@Override
	public List<ServiceDefinition> makeServiceDefinitions() {
		Future<List<CatalogItemSummary>> pageFuture = admin.getCatalogApplications();
		List<ServiceDefinition> definitions = new ArrayList<>();
		Map<String, String> version = new HashMap<>();
        Set<String> names = Sets.newHashSet();

        List<CatalogItemSummary> page = ServiceUtil.getFutureValueLoggingError(pageFuture);
        for (CatalogItemSummary app : page) {
			try {
				String id = app.getSymbolicName();
				String name = ServiceUtil.getUniqueName("br_" + app.getName(), names);
				// only take the most recent version
				if (version.containsKey(name)) {
					if (new NaturalOrderComparator().compare(app.getVersion(), version.get(name)) <= 0) {
						// don't add to catalog
						continue;
					}
				}
				version.put(name, app.getVersion());
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
				Map<String, Object> metadata = getServiceDefinitionMetadata(app.getIconUrl(), app.getPlanYaml());
				List<String> requires = getTags();
				DashboardClient dashboardClient = null;

				definitions.add(new ServiceDefinition(id, name, description,
						bindable, planUpdatable, plans, tags, metadata,
						requires, dashboardClient));
			} catch (Exception e) {
				LOG.error("unable to add catalog item: {}", e.getMessage());
			}
		}
        return definitions;
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
