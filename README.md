# Cloud Foundry Brooklyn Service Broker
[![Build Status](https://travis-ci.org/cloudfoundry-incubator/brooklyn-service-broker.svg?branch=master)](https://travis-ci.org/cloudfoundry-incubator/brooklyn-service-broker)

This project makes a CF broker which makes [Apache Brooklyn](http://brooklyn.io) blueprints 
available as Cloud Foundry services.

## Quick Start

The WAR file can be downloaded as a release on this github site,
or you can [build it from source](docs/build-and-test.md).

You can [launch](docs/launch.md) it by deploying to Cloud Foundry,
passing a few environment variables (e.g. in your `manifest.yml`):

        BROOKLYN_URI: https://brooklyn-uri:8081
        BROOKLYN_USERNAME: brooklyn-username
        BROOKLYN_PASSWORD: brooklyn-password
        SECURITY_USER_NAME: broker-username
        SECURITY_USER_PASSWORD: broker-password
  
You then [use](use.md) it by running `cf create-service-broker` and `enable-service-access` in the usual way.


## For More Information

See the [docs](docs/) in this project. Or come talk to the authors at `#brooklyncentral` on IRC.
