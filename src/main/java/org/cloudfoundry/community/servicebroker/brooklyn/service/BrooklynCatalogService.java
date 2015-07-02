package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import brooklyn.rest.domain.CatalogLocationSummary;
import brooklyn.util.text.NaturalOrderComparator;
import brooklyn.util.yaml.Yamls;

@Service
public class BrooklynCatalogService implements CatalogService {
    
    private static final Logger LOG = LoggerFactory.getLogger(BrooklynCatalogService.class);

	@Autowired
	private BrooklynRestAdmin admin;

	@Override
	public Catalog getCatalog() {
	    LOG.info("Getting catalog");
		List<CatalogItemSummary> page = admin.getCatalogApplications();
		List<ServiceDefinition> definitions = new ArrayList<ServiceDefinition>();
		Map<String, String> version = new HashMap<String, String>();
		
		for (CatalogItemSummary app : page) {
			
			String id = app.getId();
			String name = app.getName();
			
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

	private List<String> getTags() {
		return Arrays.asList();
	}

	private List<Plan> getPlans(String serviceId, String yaml) {
	    System.out.println("Getting plans");
		List<Plan> plans = new ArrayList<Plan>();
		// check if yaml contains a location
		// if it does extract that and use it
		// as the plan.
		if (yaml != null) {
			Iterator<Object> iterator = Yamls.parseAll(yaml).iterator();
			while (iterator.hasNext()) {
				Object next = iterator.next();
				if (next instanceof Map){
					Map<String, Object> map = (Map<String, Object>) next;
					if (map.containsKey("location")){
						Object location = map.get("location");
						if (location instanceof Map){
							// just use the keys
							for(String s : ((Map<String, Object>) location).keySet()){
								String id = serviceId + "." + s;
								String name = s;
								String description = "The location on which to deploy this service";
								Map<String, Object> metadata = new HashMap<String, Object>();
								plans.add(new Plan(id, name, description, metadata));
							}
						} else if (location instanceof String){
							String id = serviceId + "." + location;
							String name = (String) location;
							String description = "The location on which to deploy this service";
							Map<String, Object> metadata = new HashMap<String, Object>();
							plans.add(new Plan(id, name, description, metadata));
						}
						return plans;
					}
				}
			}
		}

		List<CatalogLocationSummary> locations = admin.getLocations();
		
		for (CatalogLocationSummary l : locations) {
			String id = serviceId + "." + l.getName();
			String name = l.getName();
			String description = "The location on which to deploy this service";
			Map<String, Object> metadata = new HashMap<String, Object>();
			plans.add(new Plan(id, name, description, metadata));
		}
		return plans;
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
