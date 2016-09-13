package org.cloudfoundry.community.servicebroker.brooklyn.service;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cloudfoundry.community.servicebroker.brooklyn.service.plan.CatalogPlanStrategy;
import org.springframework.cloud.servicebroker.model.Catalog;
import org.springframework.cloud.servicebroker.model.Plan;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.springframework.cloud.servicebroker.service.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BrooklynCatalogService implements CatalogService {
    
    private static final Logger LOG = LoggerFactory.getLogger(BrooklynCatalogService.class);

	private CatalogPlanStrategy planStrategy;

    // Catalog cache
    private Catalog catalogCache;
    private Lock cacheLock = new ReentrantLock();

	@Autowired
	public BrooklynCatalogService(CatalogPlanStrategy planStrategy) {
        this.planStrategy = planStrategy;
    }
	
	public void setPlanStrategy(CatalogPlanStrategy planStrategy) {
        this.planStrategy = planStrategy;
    }

    /*
        reserved for when the catalog is to be updated.
        use the cache to get catalog between updates.
     */
	@Override
	public Catalog getCatalog() {
	    LOG.info("Getting catalog");
        try {
            cacheLock.lock();
            catalogCache = new Catalog(planStrategy.makeServiceDefinitions());
            return catalogCache;
        } finally {
            cacheLock.unlock();
        }
	}

    public List<Plan> getPlans(String id, String planYaml) {
        return planStrategy.makePlans(id, "", planYaml);
    }
    
	@Override
	public ServiceDefinition getServiceDefinition(String serviceId) {
        try {
            cacheLock.lock();
            if (catalogCache == null) catalogCache = getCatalog();
            for (ServiceDefinition def : catalogCache.getServiceDefinitions()) {
                if (def.getId().equals(serviceId)) {
                    return def;
                }
            }
        } finally {
            cacheLock.unlock();
        }

		return null;
	}

}
