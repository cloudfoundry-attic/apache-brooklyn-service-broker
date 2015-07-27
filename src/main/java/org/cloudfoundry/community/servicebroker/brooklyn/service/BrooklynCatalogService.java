package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.CatalogPlanStrategy;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.DashboardClient;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.service.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import brooklyn.rest.domain.CatalogItemSummary;
import brooklyn.util.text.NaturalOrderComparator;

@Service
public class BrooklynCatalogService implements CatalogService {
    
    private static final Logger LOG = LoggerFactory.getLogger(BrooklynCatalogService.class);

	private BrooklynRestAdmin admin;
	private CatalogPlanStrategy planStrategy;
	
	@Autowired
	public BrooklynCatalogService(BrooklynRestAdmin admin, CatalogPlanStrategy planStrategy) {
        this.admin = admin;
        this.planStrategy = planStrategy;
    }
	
	public void setPlanStrategy(CatalogPlanStrategy planStrategy) {
        this.planStrategy = planStrategy;
    }

	@Override
	public Catalog getCatalog() {
	    LOG.info("Getting catalog");
		List<CatalogItemSummary> page = admin.getCatalogApplications();
		List<ServiceDefinition> definitions = new ArrayList<ServiceDefinition>();
		Map<String, String> version = new HashMap<String, String>();
		
		for (CatalogItemSummary app : page) {
			
			String id = app.getId();
			String name = "br_"+app.getName();
			
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
			List<Plan> plans = getPlans(id, app.getPlanYaml());
			List<String> tags = getTags();
			Map<String, Object> metadata = getServiceDefinitionMetadata(app.getIconUrl());
			List<String> requires = getTags();
			DashboardClient dashboardClient = null;

			definitions.add(new ServiceDefinition(id, name, description,
					bindable, planUpdatable, plans, tags, metadata, requires,
					dashboardClient));
		}

		return new Catalog(definitions);
	}

	public List<Plan> getPlans(String id, String planYaml) {
        return planStrategy.makePlans(id, planYaml);
    }

    private List<String> getTags() {
		return Arrays.asList();
	}

	private Map<String, Object> getServiceDefinitionMetadata(String iconUrl) {
		Map<String, Object> metadata = new HashMap<String, Object>();
		metadata.put("imageUrl", iconUrl);
		return metadata;
	}

	@Override
	public ServiceDefinition getServiceDefinition(String serviceId) {
		for (ServiceDefinition def : getCatalog().getServiceDefinitions()) {
			if (def.getId().equals(serviceId)) {
				return def;
			}
		}
		return null;
	}

}
