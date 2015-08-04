package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.List;

import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.CatalogPlanStrategy;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.service.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BrooklynCatalogService implements CatalogService {
    
    private static final Logger LOG = LoggerFactory.getLogger(BrooklynCatalogService.class);

	private CatalogPlanStrategy planStrategy;
	
	@Autowired
	public BrooklynCatalogService(CatalogPlanStrategy planStrategy) {
        this.planStrategy = planStrategy;
    }
	
	public void setPlanStrategy(CatalogPlanStrategy planStrategy) {
        this.planStrategy = planStrategy;
    }

	@Override
	public Catalog getCatalog() {
	    LOG.info("Getting catalog");
		return new Catalog(planStrategy.makeServiceDefinitions());
	}

    public List<Plan> getPlans(String id, String planYaml) {
        return planStrategy.makePlans(id, planYaml);
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
