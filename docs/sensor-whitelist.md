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
