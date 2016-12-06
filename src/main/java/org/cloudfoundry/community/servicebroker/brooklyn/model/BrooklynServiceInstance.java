package org.cloudfoundry.community.servicebroker.brooklyn.model;

import org.springframework.cloud.servicebroker.model.OperationState;
import org.springframework.core.style.ToStringCreator;

public class BrooklynServiceInstance {

    private String serviceInstanceId;
    private String serviceDefinitionId;
    private String planId;
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

    public String getPlanId() {
        return planId;
    }

    public String getOperation() {
        return operation;
    }

    public OperationState getOperationState() { return operationState; }

    public BrooklynServiceInstance withPlanId(String planId) {
        this.planId = planId;
        return this;
    }

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

    @Override
    public String toString() {
        return new ToStringCreator(this)
                .append("serviceInstanceId", serviceInstanceId)
                .append("serviceDefinitionId", serviceDefinitionId)
                .append("planId", planId)
                .append("entityId", entityId)
                .append("operation", operation)
                .append("operationState", operationState)
                .toString();
    }
}
