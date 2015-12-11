
# Using the Broker

## Setting up your catalog

First clear the catalog of any unwanted catalog items - especially if setting up brooklyn for the first time, as the default catalog items are unsuitable for running on CF without modification.

Then if using [size-plan](plans.md) mode add any `broker.config` sections to the catalog yaml to define Cloud Foundry plans before adding it.  

## Registering the Broker and Servcies

First, register the service broker

    $ cf create-service-broker <broker-name> <user> <password> <url>
    
Pass the security user and password you set for the broker, and the URL where the broker is running.

You'll now need to enable access to services.
Check for any new services that have no access:

    $ cf service-access 
    
Enable those services that you wish to appear in the marketplace:

    $ cf enable-service-access <service-name>

Check that they are available:

    $ cf marketplace


## Creating Services

To create a service and bind it to an application:

    $ cf create-service <service-name> <plan-name> <service-instance-id>
    $ cf bind-service my-app my-service
    
To unbind and delete a service:

    $ cf unbind-service my-app my-service
    $ cf delete-service <service-instance-id>


## Adding New Services

You can add new services to the Brooklyn catalog in the Brooklyn GUI, or by submitting a `POST` request to the `/create` endpoint of the service broker.
To make these available in the Cloud Foundry marketplace:

    $ cf update-service-broker <broker-name> <user> <password> <url>

And then repeat the `enable-service-access` command above.
