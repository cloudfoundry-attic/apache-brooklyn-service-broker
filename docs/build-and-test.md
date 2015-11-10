
# Building the Project

To build this project you need:

- JDK 8
- Gradle 2.2 or higher

First, install the parent project to your local maven repo:

    $ git clone https://github.com/robertgmoss/spring-boot-cf-service-broker.git
    $ cd spring-boot-cf-service-broker
    $ git checkout async
    $ ./gradlew publishToMavenLocal
    
Now you can build this project:

    $ git clone https://github.com/cloudfoundry-incubator/brooklyn-service-broker.git
    $ cd brooklyn-service-broker
    $ ./gradlew clean build

This will pull all needed dependencies, including the Brooklyn JARs (only the REST client project and dependencies are needed).

This will generate a WAR file in `build/libs/brooklyn-service-broker.war` which can be
[launched](launch.md) in Cloud Foundry (in or most any appserver).


# Testing
 
You can launch the service broker for testing using gradle's `bootRun`, as follows.

## Launching with bootRun

You will need a running Brooklyn instance, either local or injecting its URL.
Put this server's details into the `application.properties` file:

    brooklyn.uri=<brooklyn-uri>
    brooklyn.username=<brooklyn-username>
    brooklyn.password=<brooklyn-password>
    
(If you are running both Brooklyn and the Service Broker on localhost, the username and password are not required.)

Then do a `gradle clean bootRun`.
The project will build and launch a REST API on port 8080.
By default the project will create a user called `user` and generate a password.

    $ gradle clean bootRun
    ...

    Using default security password: eb940e21-9c27-4f99-b27f-9692b71c40e0

    ...

You can override this by setting the username and password in the `application.properties` file

    security.user.name=<new-username>
    security.user.password=<password>
    
Make a note of these details. You'll need them to set up the broker with `cf` (below).


## Making REST Calls

To test, it can be useful to make REST calls against this instance directly,
bypassing the CF cloud controller.

To get the catalog:

    $ export PASSWORD=<the generated password>
    $ curl http://user:$PASSWORD@localhost:8080/v2/catalog
    
And to create a WebClusterDatabaseExample from the catalog,
    
    $ curl http://user:$PASSWORD@localhost:8080/v2/service_instances/1234 -H "Content-Type: application/json" -d '{ "service_id": "brooklyn.demo.WebClusterDatabaseExample", "plan_id": "brooklyn.demo.WebClusterDatabaseExample.localhost", "organization_guid": "300","space_guid":"400"}' -X PUT

And to the delete it,

    $ curl "http://user:$PASSWORD@localhost:8080/v2/service_instances/1234?service_id=brooklyn.demo.WebClusterDatabaseExample&plan_id=brooklyn.demo.WebClusterDatabaseExample.localhost" -X DELETE
    
Binding,

    $ curl http://user:$PASSWORD@localhost:8080/v2/service_instances/1234/service_bindings/1234 -H "Content-Type: application/json" -d '{ "service_id": "brooklyn.demo.WebClusterDatabaseExample", "plan_id": "brooklyn.demo.WebClusterDatabaseExample.localhost", "app_guid":"400"}' -X PUT
    
And unbinding,

    $ curl "http://user:$PASSWORD@localhost:8080/v2/service_instances/1234/service_bindings/1234?service_id=brooklyn.demo.WebClusterDatabaseExample&plan_id=brooklyn.demo.WebClusterDatabaseExample.localhost" -X DELETE

### Extensions to the Service Broker API

There are a number of extentions that allow further communication with Brooklyn.  For example, these are used by the Brooklyn plugin for Cloud Foundry.  You can test these, too.

create a catalog entry contained in `catalog.yaml`,

    $ curl http://user:$PASSWORD@localhost:8080/create --data-binary @catalog.yaml

delete from catalog,

    $ curl -X DELETE http://user:$PASSWORD@localhost:8080/delete/brooklyn.demo.WebClusterDatabaseExample/1.0/
    
get the list of sensors,

    $ curl http://user:$PASSWORD@localhost:8080/sensors/1234
    
check if a service is running,

    $ curl http://user:$PASSWORD@localhost:8080/is-running/1234
    
get the list of effectors,

    $ curl http://user:$PASSWORD@localhost:8080/effectors/1234

invoke an effector,

    $ curl http://user:$PASSWORD@localhost:8080/invoke/{entity}/{effector} -H "Content-Type: application/json" -d '{ "parameter name" : "parameter value" }' -X POST
    
