package org.cloudfoundry.community.servicebroker.brooklyn.repository;

import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BrooklynServiceInstanceBindingRepository extends MongoRepository<ServiceInstanceBinding, String>{
	
}
