
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
 
