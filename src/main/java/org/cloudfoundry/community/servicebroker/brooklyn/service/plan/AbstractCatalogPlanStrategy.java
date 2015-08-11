package org.cloudfoundry.community.servicebroker.brooklyn.service.plan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.brooklyn.rest.domain.CatalogItemSummary;
import org.cloudfoundry.community.servicebroker.brooklyn.service.BrooklynRestAdmin;
import org.cloudfoundry.community.servicebroker.brooklyn.service.ServiceUtil;
import org.cloudfoundry.community.servicebroker.model.DashboardClient;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.springframework.beans.factory.annotation.Autowired;

import brooklyn.util.text.NaturalOrderComparator;

import com.google.common.collect.Sets;

public abstract class AbstractCatalogPlanStrategy implements CatalogPlanStrategy{
	
	private BrooklynRestAdmin admin;
	
	@Autowired
	public AbstractCatalogPlanStrategy(BrooklynRestAdmin admin) {
		this.admin = admin;
	}
	
	protected BrooklynRestAdmin getAdmin() {
		return admin;
	}
	
	@Override
	public List<ServiceDefinition> makeServiceDefinitions() {
		Future<List<CatalogItemSummary>> pageFuture = admin.getCatalogApplications();
		List<ServiceDefinition> definitions = new ArrayList<>();
		Map<String, String> version = new HashMap<>();
        Set<String> names = Sets.newHashSet();

        List<CatalogItemSummary> page = ServiceUtil.getFutureValueLoggingError(pageFuture);
        for (CatalogItemSummary app : page) {
			
			String id = app.getId();
			String name = ServiceUtil.getUniqueName("br_"+app.getName(), names);
			// only take the most recent version
			if (version.containsKey(name)){
				if (new NaturalOrderComparator().compare(app.getVersion(), version.get(name)) <= 0){
					// don't add to catalog
					continue;
				}
			}
			version.put(name, app.getVersion());
			String description = app.getDescription();
			boolean bindable = true;
			boolean planUpdatable = false;
			List<Plan> plans = makePlans(id, app.getPlanYaml());
            if (plans.size() == 0) {
                continue;
            }
			List<String> tags = getTags();
			Map<String, Object> metadata = getServiceDefinitionMetadata(app.getIconUrl(), app.getPlanYaml());
			List<String> requires = getTags();
			DashboardClient dashboardClient = null;

			definitions.add(new ServiceDefinition(id, name, description,
					bindable, planUpdatable, plans, tags, metadata, requires,
					dashboardClient));
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
