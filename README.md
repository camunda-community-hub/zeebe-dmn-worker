# zeebe-dmn-worker

[![](https://img.shields.io/badge/Community%20Extension-An%20open%20source%20community%20maintained%20project-FF4700)](https://github.com/camunda-community-hub/community)

[![](https://img.shields.io/badge/Lifecycle-Stable-brightgreen)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#stable-)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Zeebe worker to evaluate DMN decisions (i.e. business rule tasks). It uses the [Camunda DMN engine](https://docs.camunda.org/manual/7.12/reference/dmn11/) including the [FEEL-Scala engine](https://github.com/camunda/feel-scala) to evaluate DMN decisions. The DMN files are read from a local directory.

## Usage

Example BPMN with service task:

```xml
<bpmn:serviceTask id="decisionTask" name="Eval DMN decision">
  <bpmn:extensionElements>
    <zeebe:taskDefinition type="DMN" />
    <zeebe:taskHeaders>
      <zeebe:header key="decisionRef" value="dish-decision" />
    </zeebe:taskHeaders>
    <zeebe:ioMapping>
      <zeebe:output source="result" target="decisionResult" />
    </zeebe:ioMapping>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

* the worker is registered for the type `DMN`
* required custom headers:
    * `decisionRef` - the id of the decision to evaluate
* the result of the evaluation is passed as `result` variable

## Install

### Docker

The docker image for the worker is published to [GitHub Packages](https://github.com/orgs/camunda-community-hub/packages/container/package/zeebe-dmn-worker).

```
docker pull ghcr.io/camunda-community-hub/zeebe-dmn-worker:1.1.0
```
* configure the connection to the Zeebe gateway by setting `zeebe.client.broker.gatewayAddress` (default: `localhost:26500`) 
* configure the folder where the DMN files are located by setting `zeebe.client.worker.dmn.repository` (default: `dmn-repo`)

For a local setup, the repository contains a [docker-compose file](docker/docker-compose.yml). It starts a Zeebe broker with an embedded gateway and the worker. 

```
cd docker
docker-compose up
```

### Manual

1. Download the latest [worker JAR](https://github.com/camunda-community-hub/zeebe-dmn-worker/releases) _(zeebe-dmn-worker-%{VERSION}.jar
)_

1. Start the worker
    `java -jar zeebe-dmn-worker-{VERSION}.jar`

### Configuration

The worker is a Spring Boot application that uses the [Spring Zeebe Starter](https://github.com/zeebe-io/spring-zeebe). The configuration can be changed via environment variables or an `application.yaml` file. See also the following resources:
* [Spring Zeebe Configuration](https://github.com/zeebe-io/spring-zeebe#configuring-zeebe-connection)
* [Spring Boot Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config)

For configuration with Camunda Cloud SaaS, open the 'application.yaml' file located in `src/main/resources` and remove the '#' sign from lines 3-6. Replace the placeholder values for `clusterId`, `clientId`, and `clientSecret` with your cloud credentials. Follow the instructions provided on line 15 as well.

By default, the DMN files are read from the folder `dmn-repo` next to the application (i.e. the working directory).

```
zeebe:
  client:
    worker:
      defaultName: camunda-dmn-worker
      defaultType: DMN
      threads: 3
    
      dmn.repository: dmn-repo

    job.timeout: 10000
    broker.gatewayAddress: 127.0.0.1:26500
    security.plaintext: true
```

## Build from Source

Build with Maven

`mvn clean install`

## Code of Conduct

This project adheres to the Contributor Covenant [Code of
Conduct](/CODE_OF_CONDUCT.md). By participating, you are expected to uphold
this code. Please report unacceptable behavior to
code-of-conduct@zeebe.io.
