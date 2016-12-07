# carbon-multitenancy

---

|  Branch | Build Status |
| :------------ |:-------------
| master      | [![Build Status](https://wso2.org/jenkins/job/carbon-multitenancy/badge/icon)](https://wso2.org/jenkins/job/carbon-multitenancy) |


---

Latest Released Version v4.5.0, v4.4.4.

#### carbon-multitenancy repo contains the the following component.

* tenant-mgt

The goal of multitenancy is to maximize resource sharing by allowing multiple users (tenants) to log in and use a single sever/cluster at the same time, in a tenant-isolated manner. That is, each user is given the experience of using his/her own server, rather than a shared environment. Multitenancy ensures optimal performance of the system's resources such as memory and hardware and also secures each tenant's personal data.

This repository contains the features required for multitenancy functionality.

Following endpoints are exposed in the tenant management API.

```    
GET /tenants
```
Produces: `application/json`
Response: List of tenants
    
```
GET /tenants/{name}
```
Produces: `application/json`
Response: Tenant object
```
POST /tenants
```
Consumes: `application/json`
Request Body: Tenant object
    
```
DELETE /tenants/{name}
```

###### Models:

Tenant model
```
{
    name: String
}
```

* deployment-automation

The goal of deployment automation is to provide easy to use API which can be used to deploy WSO2 products in a containerized environment.

This repository contains featured required for deployment automation functionality.

Following endpoints are exposed in the deployment automation API.

```
GET /deployments?platform={platform}
```

Produces: `application/json`
Response: List of deployments

```
GET /deployments/{id}?platform={platform}
```

Produces: `application/json`
Response: Deployment object

```
POST /deployments
```

Consumes: `application/json`
Request Body: Deployment object 

```
DELETE /deployments
```

Consumes: `application/json`
Request Body: Deployment object

###### Models:

Deployment model
```
{
    id: String
    product: String
    version: String
    pattern: Integer
    platform: String
}
```

## How to Contribute
* Please report issues at [Carbon JIRA] (https://wso2.org/jira/browse/CARBON).
* Send your bug fixes pull requests to [master branch] (https://github.com/wso2/carbon-multitenancy/tree/master)

## Contact us
WSO2 Carbon developers can be contacted via the mailing lists:

* Carbon Developers List : dev@wso2.org
* Carbon Architecture List : architecture@wso2.org
