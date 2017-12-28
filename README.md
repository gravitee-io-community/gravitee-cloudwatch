[![Build Status](https://ci.gravitee.io/buildStatus/icon?job=gravitee-io/gravitee-reporter-es)](https://ci.gravitee.io/job/gravitee-io/job/gravitee-reporter-es/job/master/)

# gravitee-reporter-cloudwatch

The project is organized in two modules:
* gravitee-service-cloudwatch 
* gravitee-reporter-cloudwatch


Service (gravitee-service-cloudwatch): if enabled (false per default) creates a dashboard on AWS CloudWatch with the name specified
in gravitee.yaml file (if not already present) and creates the widget for the specific API Gateway instance

Reporter (gravitee-reporter-cloudwatch): if enabled (false per default) send metrics to AWS CloudWatch


## Build

This plugin require :  

* Maven 3
* JDK 8

Once built, a plugin archive file is generated in : target/gravitee-reporter-cloudwatch-1.0.0-SNAPSHOT.zip
Once built, a plugin archive file is generated in : target/gravitee-service-cloudwatch-1.0.0-SNAPSHOT.zip


## Deploy

Just unzip the plugins archive in your gravitee plugin workspace ( default is : ${gravitee.home}/plugins )


## Configuration 

The configuration is loaded from the common GraviteeIO Gateway configuration file (gravitee.yml)


Example : 

```YAML
reporters:
  cloudwatch:
    enabled: true
    awsRegion: ${ds.aws.awsRegion}
    awsAccessKeyId: ${ds.aws.awsAccessKeyId}
    awsSecretKey: ${ds.aws.awsSecretKey} 
    
services:
  cloudwatch:
    enabled: true # Is the service enabled or not (default to false)
    dashboardName: API-Gateway-Dashboard
    lifecycleQueueName: lifecycleQueueName
    awsRegion: ${ds.aws.awsRegion}
    awsAccessKeyId: ${ds.aws.awsAccessKeyId}
    awsSecretKey: ${ds.aws.awsSecretKey} 
    
# Referenced properties
ds:
  aws:
    awsRegion: your aws account region
    awsAccessKeyId: your aws access key id
    awsSecretKey: your aws secret key
```
