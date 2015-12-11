
# Deploying the Brooklyn Service Broker

You can deploy the WAR using Cloud Foundry,
or to your favourite appserver, or using Brooklyn itself.

The only configuration is to specify the Brooklyn endpoint and credentials, 
and the security credentials which will be needed to access the broker. 


## Deploying with Cloud Foundry

Create an application manifest, including environment variables pointing at the Brooklyn endpoint
and specifying the security credentials which will be needed to use this broker:

    applications:
    - name: Brooklyn-Service-Broker
      env:
        BROOKLYN_URI: https://brooklyn-uri:8081
        BROOKLYN_USERNAME: brooklyn-username
        BROOKLYN_PASSWORD: brooklyn-password
        SECURITY_USER_NAME: broker-username
        SECURITY_USER_PASSWORD: broker-password
        BROOKLYN_NAMESPACE: br

then 

    $ cf push -p path/to/brooklyn-service-broker.war -b https://github.com/cloudfoundry/java-buildpack.git

(If you've built the project locally, the WAR will be in `build/libs/`.)


## Deploying on Brooklyn

The following blueprint can be used to have brooklyn deploy the service broker:

    name: Brooklyn Service Broker
    
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
          
    location: 
      jclouds:aws-ec2:
        identity: <REPLACE>
        credential: <REPLACE>

Take care to replace:

* the path to the WAR
* the location properties (any of the Brooklyn machine locations can be used)

And optionally, if you're expecting very high load, replace the `TomcatServer` with
the `brooklyn.entity.webapp.ControlledDynamicWebAppCluster`.

 
## Next

You're ready to set up the broker for [use](use.md) from Cloud Foundry.
