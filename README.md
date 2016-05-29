[![Build Status](https://travis-ci.org/vpechorin/usense.svg?branch=master)](https://travis-ci.org/vpechorin/usense)

# usense


  Java Service Discovery service utilising [NATS](http://nats.io) message broker

## Provides

    A consistent, highly available service directory

    A mechanism to register/deregister services

    A mechanism to perform directs lookups

    A mechanism to provide broadcast BROWSE request to ask every service to responde

    This lightweight library should be attached to the each microservice. It will maintain service registry
    and perform communications with other microservices use NATS as a message broker.

## Usage

```java
    // This will create usense instance, send registration message and perform initial browse lookup
    // `servicehnd` - is the name of your service (String)
    // `localhost` - your service hostname/address (String)
    // `8081` - your service port (int)
    // `nats://localhost:4222` - NATS message broker address
    Usense usense = Usense.newClient("servicehnd", "localhost", 8081, "nats://localhost:4222");

    // Find the one service
    ServiceInstance service = usense.findService("servicehnd");

    // Find another service
    ServiceInstance service = usense.findService("service2");

    // Find all service instances for the specified name
    List<ServiceInstance> services = findAllServiceInstances("servicehnd")

    // Finally
    // Deregister the current service
    usense.unregister()
```
