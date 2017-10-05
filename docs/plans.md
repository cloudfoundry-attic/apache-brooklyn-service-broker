# Using Catalog Plan Strategies

Catalog Plan Strategies are responsible for creating the plans associated with catalog items. There are currently
two types of catalog plan strategy: `SizePlanStrategy` and `LocationPlanStrategy`.  These are set in the properties 
used by the broker, e.g. if you have deployed the service broker to cloud foundry, these are set in the `manifest.yaml` 
file using the `SPRING_PROFILES_ACTIVE` variable.

## SizePlanStrategy

In this example, the size plan strategy has been selected:

```
applications:
- name: Brooklyn-Service-Broker
  memory: 1G
  env:
    BROOKLYN_URI: http://my.brooklyn.server:8081
    BROOKLYN_USERNAME: admin
    BROOKLYN_PASSWORD: letmein
    SECURITY_USER_NAME: user
    SECURITY_USER_PASSWORD: password
    SPRING_PROFILES_ACTIVE: size-plan
    BROOKLYN_LOCATION: aws-cloudfoundry
```

The size plan strategy allows a location to be set in the `BROOKLYN_LOCATION` variable. 
The plans are determined by the blueprints in the brooklyn catalog in the `broker.config` section. An example
`broker.config` section is as follows:

```
brooklyn.catalog:
  id: com.development.mongodb
  version: 1.0 

...

brooklyn.config:
  broker.config:
    plans:
    - name: small
      description: Single Node
      plan.config:
        cluster.initial.size: 1
    - name: medium
      description: Three-node replica set
      plan.config:
        cluster.initial.size: 3
    - name: large
      description: Five-node replica set
      plan.config:
        cluster.initial.size: 5
services:

...

```

In this case, three plans will be created for the blueprint, `small`, `medium`, and `large`. The configuration options
within each plan are included in the `brooklyn.config` section when deploying the blueprint, so e.g. when deploying
a `small` plan, the following JSON will be created to deploy the instance:

```
{
  "services": ["type": "com.development.mongodb:1.0"],
  "locations": [
    "aws-cloudfoundry"
  ],
  "brooklyn.config": {
    "cluster.initial.size": 1
  }
}
```

### Metadata and Tags

In addition to setting the plans of a service, additional metadata and tags can be set as follows:

```
brooklyn.config:
  broker.config:
    metadata:
      key1: value1
      key2: value2
    tags:
    - tag1
    - tag2
    plans:
      ...
```


## LocationPlanStrategy

The location plan strategy is activated by using the SPRING_PROFILES_ACTIVE value of `location-plan` as follows:

```
applications:
- name: Brooklyn-Service-Broker
  memory: 1G
  env:
    BROOKLYN_URI: http://my.brooklyn.server:8081
    BROOKLYN_USERNAME: admin
    BROOKLYN_PASSWORD: letmein
    SECURITY_USER_NAME: user
    SECURITY_USER_PASSWORD: password
    SPRING_PROFILES_ACTIVE: location-plan
```

This will create a (service, plan) pair for each blueprint defined in the Brooklyn catalog for each of the locations
defined in Brooklyn. E.g. if there are four blueprints defined, and five locations defined, a total of 20
service plan pairs will be created.

# Apache Brooklyn Blueprint Service
The default service included with the Brooklyn Service Broker allows users to create new service instances without 
adding to the Apache Brooklyn Catalog; the blueprint is supplied as a parameter to the `create-service` command as a
JSON object.

An example is as follows:

```
cf create-service br_apache_brooklyn_blueprint default my-user-defined-service -c '{"services":[{"type": "org.apache.brooklyn.entity.nosql.mongodb.MongoDBServer"}], "location":"aws-cloudfoundry"}'
```

# Brooklyn catalog versions

When using eith the location or size plan strategy, the Brooklyn service broker will create plans only for the most
recent versions of blueprints. This default behaviour can be overridden by specifying the BROOKLYN_ALL_CATALOG_VERSION
variable in the manifest as follows:

```
applications:
- name: Brooklyn-Service-Broker
  memory: 1G
  env:
    BROOKLYN_URI: http://my.brooklyn.server:8081
    BROOKLYN_USERNAME: admin
    BROOKLYN_PASSWORD: letmein
    SECURITY_USER_NAME: user
    SECURITY_USER_PASSWORD: password
    SPRING_PROFILES_ACTIVE: location-plan
    BROOKLYN_ALL_CATALOG_VERSION: true
```


# Development mode (plan)

Development mode can be activated to allow the Brooklyn service broker to connect to a Brooklyn server that has
been set up to use https with a self-signed certificate by specifying the development profile in addition to
the location or size plan:


```
applications:
- name: Brooklyn-Service-Broker
  memory: 1G
  env:
    BROOKLYN_URI: https://my.brooklyn.server:8081
    BROOKLYN_USERNAME: admin
    BROOKLYN_PASSWORD: letmein
    SECURITY_USER_NAME: user
    SECURITY_USER_PASSWORD: password
    SPRING_PROFILES_ACTIVE: location-plan,development
```

# Namespaces

Sometimes your Service names will conflict, especially when deploying two brokers that target one brooklyn or two brooklyns with the same services. To ensure service names are unique add a namespace as follows:


```
applications:
- name: Brooklyn-Service-Broker
  memory: 1G
  env:
    BROOKLYN_URI: https://my.brooklyn.server:8081
    BROOKLYN_USERNAME: admin
    BROOKLYN_PASSWORD: letmein
    SECURITY_USER_NAME: user
    SECURITY_USER_PASSWORD: password
    SPRING_PROFILES_ACTIVE: location-plan,development
    BROOKLYN_NAMESPACE: br
```
