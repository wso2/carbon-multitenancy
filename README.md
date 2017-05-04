# Carbon Multitenancy

---
|  Branch       | Build Status |
| :------------ |:-------------
| master        | [![Build Status](https://wso2.org/jenkins/view/All%20Builds/job/platform-builds/job/carbon-multitenancy/badge/icon)](https://wso2.org/jenkins/view/All%20Builds/job/platform-builds/job/carbon-multitenancy/) |
---

Latest Released Versions: v4.5.0, v4.4.4.

In Carbon 5 multitenancy is handled using containers by creating a dedicated set of containers for each tenant.

This can be implemented using [Kubernetes](https://kubernetes.io) by using its [namespaces](https://kubernetes.io/docs/user-guide/namespaces/) 
feature. According to this approach each tenant will have its own namespace and completely isolated environments for creating containers including network level isolation.

Carbon Multitenancy provides a container cluster manager agnostic API for tenant management and deployment automation for WSO2 products. Currently it supports only Kubernetes.

## Tenant Management

The following API resources are provided for managing tenants on container cluster management platforms:

- Create a tenant: HTTP POST /tenants
- Get all tenants: HTTP GET /tenants
- Get a tenant by name: HTTP GET /tenants/{name}
- Delete a tenant by name: HTTP DELETE /tenants/{name}

### Tenant Model

The tenant model is defined as follows:

```json
{
    "name": "String"
}
```

## Deployment Automation

The following API resources are provided for deploying WSO2 products on container cluster management platforms:

- Create a deployment: POST /deployments
- Get all deployments: GET /deployments
- Get a deployment by id: GET /deployments/{id}
- Delete a deployment by id: DELETE /deployments/{id}


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

## How to Run

- Download the latest Carbon Multitenancy distribution or build this project using Maven:
  ````
  cd carbon-multitenancy/
  mvn clean install
  ````

- Download and extract [WSO2 Kubernetes Artifacts](https://github.com/wso2/kubernetes-artifacts) and set the following environment variable:
  
  ````
  export WSO2_KUBERNETES_ARTIFACTS_PATH=/path/to/kubernetes-artifacts/

  ````

- Download and extract [WSO2 Identity Server](http://wso2.com/products/identity-server) 5.1.0.

- Download and copy [introspect.war](https://github.com/wso2/msf4j/blob/v2.2.1/samples/oauth2-security/resources/introspect.war) to wso2is-5.1.0/repository/deployment/server/webapps directory.

- Start WSO2 Identity Server:
  
  ````
  cd [wso2is-5.1.0/bin]
  ./wso2server.sh
  ````

- Create a service provider by following the instructions in the [this document](https://docs.wso2.com/display/IS510/Configuring+a+Service+Provider).

- Under the section "Configure Inbound Authentication", create an OAuth2 application which represents your
client application. Instructions are available in the above documentation link. For "Callback URL", provide 
"https://localhost:9443/oauth2/token".

- Copy OAuth Client Key and OAuth Client Secret from the above UI and execute the below command for generating an OAuth token:
  
  ````
  curl -v -k -X POST --basic -u <OAuth Client Key>:<OAuth Client Secret> \
  -H "Content-Type: application/x-www-form-urlencoded;charset=UTF-8" \
  -d "grant_type=client_credentials" https://localhost:9443/oauth2/token
  ````

- Set OAuth2 token verification URL via an environment variable as follows:
  
  ````
  export AUTH_SERVER_URL=http://localhost:9763/introspect
  ````
  
- Start Carbon Multitenancy API using the following command:
  
  ````
  java -Dtransports.netty.conf=resources/conf/netty-transports.yml \
  -Djavax.net.ssl.trustStore"=resources/wso2carbon.jks" \
  -Djavax.net.ssl.trustStorePassword="wso2carbon" \
  -Djavax.net.ssl.trustStoreType="JKS" \
  -jar components/org.wso2.carbon.multitenancy.application/target/carbon-multitenancy-application-*.jar
  ````

- Execute the below command to verify the Cabon Multitenancy API:

  ````
  curl -v -H "Authorization: Bearer <OAuth Token>" -H "Content-Type: application/json" -d '{ "name":"foo" }' http://localhost:8080/tenants
  ````

- Verify namespace creation using Kubernetes CLI:

  ````
  kubectl get namespaces

  NAME          STATUS    AGE
  default       Active    5h
  foo           Active    23s
  kube-system   Active    5h
  ````

## How to Contribute

* Please report issues at [Carbon JIRA] (https://wso2.org/jira/browse/CARBON).
* Send your bug fixes pull requests to [master branch] (https://github.com/wso2/carbon-multitenancy/tree/master)

## Contact us

WSO2 Carbon developers can be contacted via the mailing lists:

* Carbon Developers List : dev@wso2.org
* Carbon Architecture List : architecture@wso2.org
