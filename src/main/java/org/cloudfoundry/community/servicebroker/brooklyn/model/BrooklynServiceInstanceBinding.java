package org.cloudfoundry.community.servicebroker.brooklyn.model;

import java.util.Map;

public class BrooklynServiceInstanceBinding {
    private final String serviceBindingId;
    private final String serviceInstanceId;
    private final Map<String, Object> credentials;
    private final String appGuid;
    private final String entityId;

    public BrooklynServiceInstanceBinding(String serviceBindingId, String serviceInstanceId, Map<String, Object> credentials, String appGuid, String entityId) {
        this.serviceBindingId = serviceBindingId;
        this.serviceInstanceId = serviceInstanceId;
        this.credentials = credentials;
        this.appGuid = appGuid;
        this.entityId = entityId;
    }

    public String getAppGuid() {
        return appGuid;
    }

    public Map<String, Object> getCredentials() {
        return credentials;
    }

    public String getServiceBindingId() {
        return serviceBindingId;
    }

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public String getEntityId() {
        return entityId;
    }
}
