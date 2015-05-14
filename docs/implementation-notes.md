
# Implementation Details

Basically, this translates from the Brooklyn catalog to the CF marketplace, 
and then implements requests from the CF service broker API to the 
corresponding actions in brooklyn:

* create-service -> deploy a Brooklyn blueprint
* bind-service -> take details of a deployed app and associate it with an application


## Other Endpoints

This server supports several other endpoints beyond those required by the service broker API, 
used by the CLI plugin:

* `/create` a catalog item, posting yaml
* `/delete/{name}/{version}` a catalog item
* `/sensors/{application}`
* `/effectors/{application}`
* `/invoke/{application}/{entity}/{effector}`
* `/is-running/{application}`


## Ideas and TODOs

### More Complex Relationships

It could be useful to support more complex relationships in certain cases,
for instance creating a database in an RDBMS on `bind-service`,
or extracting custom metadata for a particular bind.
This could be easily done by looking for a `bind-service` effector on an application,
and invoking it if present.

The new service keys API could be useful for this.


### Async and Parameters API

The latest broker API includes support for long-running (async) tasks and parameters.

We should use this to support:

* `bind` being long-running if the entities in brooklyn have not yet finished starting
* config or even arbitrary blueprints being specifiable in `create-service`
* arguments to pass to a `bind-service` effector
 