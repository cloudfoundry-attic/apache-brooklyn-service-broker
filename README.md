Cloud Foundry Brooklyn Service Broker
-------------------------------------

This project launches a CF broker which makes Brooklyn blueprints available as Cloud Foundry services.

You will need [Gradle](http://www.gradle.org/installation) and [Brooklyn](http://brooklyn.io) installed.
You will also need Java 8 -- 
if that is not your system default, and it is too rough to change your system
a standalone `jdk8` download can usually be activated in a single shell 
by setting `export JAVA_HOME=/path/to/jdk8/Home` (or similar).

Make sure Brooklyn is running, then do a `gradle clean bootRun`.
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
    
Using with the CF tool
----------------------

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
    
Deploying to Cloud Foundry
--------------------------

create an application manifest, e.g.,

    applications:
    - name: Brooklyn-Service-Broker
      env:
        BROOKLYN_URI: https://brooklyn-uri:8081
        BROOKLYN_USERNAME: brooklyn-username
        BROOKLYN_PASSWORD: brooklyn-password
        SECURITY_USER_NAME: broker-username
        SECURITY_USER_PASSWORD: broker-password
      services:
      - broker-mongodb

(if you have a MongoDB instance running elsewhere that you would like to use, configure it using SPRING_DATA_MONGODB_HOST and SPRING_DATA_MONGODB_PORT)

then 

    $ gradle clean build
    $ cf push -p build/libs/brooklyn-service-broker.war -b https://github.com/cloudfoundry/java-buildpack.git
