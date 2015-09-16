# Sensor Whitelists

Blueprints that contain a broker.config section may specify a whitelist
of sensors to be included at bind time as credentials. E.g.

```
brooklyn.config:
  broker.config:
    sensor.whitelist:
    - foo.bar
    - sensor.one.name
```

This allows additional sensors to be specified in addition to the Global
whitelist:

```
"host.name"
"host.address"
"host.sshAddress"
```

If sensors need to be included with a specific name, e.g. `host` instead
of `host.name`, a propagating enricher can be added to the entity in the
blueprint which will propagate the sensors to the desired name, e.g. the
following is used in the Rabbit blueprint:

```
  brooklyn.enrichers:
  - enricherType: brooklyn.enricher.basic.Propagator
    brooklyn.config:
      sensorMapping:
        $brooklyn:sensor("rabbitmq.management.url"): $brooklyn:sensor("management.url")
        $brooklyn:sensor("host.name"): $brooklyn:sensor("host")
        $brooklyn:sensor("amqp.virtualHost"): $brooklyn:sensor("virtual-host")
        $brooklyn:sensor("amqp.port"): $brooklyn:sensor("port")
        $brooklyn:sensor("broker.url"): $brooklyn:sensor("uri")
        $brooklyn:sensor("amqp.version"): $brooklyn:sensor("version")
```

The mapped sensors can then be added to the whitelist

# Entity Blacklists

You may also specify a blacklist of entity types, the sensors of which will
be excluded at bind time as credentials. E.g.

```
brooklyn.config:
  broker.config:
    entity.blacklist:
    - org.apache.brooklyn.entity.nosql.redis.RedisSlave
```

This allows additional entities to be specified in addition to the Global
blacklist:

```
"org.apache.brooklyn.entity.group.QuarantineGroup"
"brooklyn.networking.subnet.SubnetTier"
```
