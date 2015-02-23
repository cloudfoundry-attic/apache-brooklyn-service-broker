package org.cloudfoundry.community.servicebroker.brooklyn.repository;

import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BrooklynServiceInstanceRepository extends MongoRepository<ServiceInstance, String>{

}
