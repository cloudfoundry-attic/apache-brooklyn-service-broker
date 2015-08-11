# Using Catalog Plan Strategies

Catalog Plan Strategies are responsible for creating the plans associated with catalog items. There are currently
two types of catalog plan strategy: `SizePlanStrategy` and `LocationPlanStrategy`.  These are set in the properties 
used by the broker, e.g. if you have deployed the service broker to cloud foundry, these are set in the `manifest.yaml` 
file using the `SPRING_PROFILES_ACTIVE` variable.

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
      small:
        cluster.initial.size: 1
      medium:
        cluster.initial.size: 3
      large:
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


