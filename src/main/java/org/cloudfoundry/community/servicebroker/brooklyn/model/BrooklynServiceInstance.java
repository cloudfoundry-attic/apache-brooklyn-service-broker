package org.cloudfoundry.community.servicebroker.brooklyn.model;

import org.springframework.cloud.servicebroker.model.OperationState;

public class BrooklynServiceInstance {

    private String serviceInstanceId;
    private String serviceDefinitionId;
    private String entityId;
    private String operation;
    private OperationState operationState;

    public BrooklynServiceInstance(String serviceInstanceId, String serviceDefinitionId) {
        this.serviceInstanceId = serviceInstanceId;
        this.serviceDefinitionId = serviceDefinitionId;
    }

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public String getServiceDefinitionId() {
        return serviceDefinitionId;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getOperation() {
        return operation;
    }

    public OperationState getOperationState() { return operationState; }

    public BrooklynServiceInstance withEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public BrooklynServiceInstance withOperation(String operation) {
        this.operation = operation;
        return this;
    }

    public BrooklynServiceInstance withOperationStatus(OperationState operationState) {
        this.operationState = operationState;
        return this;
    }
}
