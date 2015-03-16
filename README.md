# Cloud Foundry Brooklyn Service Broker

This project launches a CF broker which makes Brooklyn blueprints available as Cloud Foundry services.

## Prerequisites

To build this project you need:

- Oracle JDK 8
- Gradle 2.2 or higher
- [Brooklyn 0.7.0-SNAPSHOT](https://github.com/apache/incubator-brooklyn)


Note: You should install Brooklyn 0.7.0-SNAPSHOT to your local maven repository before compiling the Service Broker, since the service broker depends on its client library. 

To launch the service broker, you will need a running Brooklyn and MongoDB (these do not have to be local, the locations of which can be specified through properties).

See the [wiki](wiki) for instruction notes on installing for specific operating systems .

## Deploying locally

Make sure Brooklyn and MongoDB is running, and put the brooklyn details into the `application.properties`

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

### Testing
You can also use this for making REST calls directly, bypassing CF, for testing.
For instance to get the catalog,

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
    
## Deploying to Cloud Foundry

Create an application manifest, e.g.,

    applications:
    - name: Brooklyn-Service-Broker
      env:
        BROOKLYN_URI: https://brooklyn-uri:8081
        BROOKLYN_USERNAME: brooklyn-username
        BROOKLYN_PASSWORD: brooklyn-password
        SECURITY_USER_NAME: broker-username
        SECURITY_USER_PASSWORD: broker-password
        SPRING_PROFILES_ACTIVE=cloud

then 

    $ gradle clean build
    $ cf push -p build/libs/brooklyn-service-broker.war -b https://github.com/cloudfoundry/java-buildpack.git
    
## Deploying on Brooklyn

You can also run the Brooklyn Service Broker from inside Brooklyn. First build the project:

    $ gradle clean build
    
then with the resulting `build/libs/brooklyn-service-broker.war` file path and properties substituted into the following blueprint, paste it into the "Add Application" dialog in your Brooklyn server.
 
    name: Brooklyn Service Broker
    location: localhost
    services:
    - type: brooklyn.entity.webapp.tomcat.TomcatServer
      name: Tomcat Server
      war: /path/to/build/libs/brooklyn-service-broker.war
      brooklyn.config:
        java.sysprops:
          brooklyn.uri: https://brooklyn-uri:8081
          brooklyn.username: brooklyn-username
          brooklyn.password: brooklyn-password
          security.user.name: broker-user
          security.user.password: broker-password
    
## Using with the CF tool

First, register the service broker

    $ cf create-service-broker <broker-name> <user> <password> <url>
    
Check for new services that have no access

    $ cf service-access 
    
Enable those services that you wish to appear in the marketplace

    $ cf enable-service-access <service-name>
    
Create the service that you wish to use

    $ cf create-service <service-name> <plan-name> <service-instance-id>
    
Delete the service that you no longer need

    $ cf delete-service <service-instance-id>

Bind the service

    $ cf bind-service my-app my-service
    
Unbind the service

    $ cf unbind-service my-app my-service
      
