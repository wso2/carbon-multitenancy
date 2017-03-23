# Carbon Multitenancy

---
|  Branch       | Build Status |
| :------------ |:-------------
| master        | [![Build Status](https://wso2.org/jenkins/view/All%20Builds/job/platform-builds/job/carbon-multitenancy/badge/icon)](https://wso2.org/jenkins/view/All%20Builds/job/platform-builds/job/carbon-multitenancy/) |
---

Latest Released Version v4.5.0, v4.4.4.

Carbon servers have become container native with the introduction of Carbon v5 (C5). As a result, In-JVM multi-tenancy 
model which was used in previous Carbon versions was removed and container multitenancy was introduced. In C5,
multitenancy is handled by creating a dedicated set of containers for each tenant.

This can be achieved on [Kubernetes](https://kubernetes.io) by using its [namespaces](https://kubernetes.io/docs/user-guide/namespaces/) 
feature. According to this approach, each tenant will have its own namespace and completely isolated environments for 
creating containers including network level isolation on a single set of container hosts.

Carbon Multitenancy provides a set of container platform agnostic APIs for managing such isolated environments on 
container platforms. Initially it supports Kubernetes and later support for other container cluster managers will be 
added.

## Tenants API

The Tenants API provides features for creating and managing tenants on container cluster management platforms for
deploying WSO2 products.

### Tenants API Resources

The following resources available in the Tenants API:

```
POST /tenants
GET /tenants
GET /tenants/{name}
DELETE /tenants/{name}
```

### Tenant Model:

The tenant model is defined as follows:

```json
{
    "name": "String"
}
```

## Deployments API

The deployments API provides features for deploying WSO2 products on container cluster management platforms.

### Deployments API Resources

The following resources are available in the Deployments API:

```
POST /deployments
GET /deployments
GET /deployments/{id}
DELETE /deployments/{id}
```


### Deployment Model

The deployment model is defined as follows:

```json
{
    "id": "String",
    "product": "String",
    "version": "String",
    "pattern": "Integer"
}
```

## How to Contribute

* Please report issues at [Carbon JIRA] (https://wso2.org/jira/browse/CARBON).
* Send your bug fixes pull requests to [master branch] (https://github.com/wso2/carbon-multitenancy/tree/master)

## Contact us

WSO2 Carbon developers can be contacted via the mailing lists:

* Carbon Developers List : dev@wso2.org
* Carbon Architecture List : architecture@wso2.org
