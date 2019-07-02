# zeebe-dmn-worker

A Zeebe worker to evaluate DMN decisions (i.e. business rule tasks). It uses the Camunda DMN engine for evaluation and a local directory to read the decisions from.

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
      <zeebe:output source="$.result" target="$.decisionResult" />
    </zeebe:ioMapping>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

* the worker is registered for the type `DMN`
* required custom headers:
    * `decisionRef` - the id of the decision to evaluate
* the result of the evaluation is passed as `result` variable

## Install

1) Download the [JAR file](https://github.com/zeebe-io/zeebe-dmn-worker/releases)

2) Execute the JAR file via

    `java -jar target/zeebe-dmn-worker.jar`

### Configuration

The connection can be changed by setting the environment variables:
* `dmn.repo` (default: `dmn-repo`)
* `zeebe.client.broker.contactPoint` (default: `127.0.0.1:26500`)

## Build from Source
   
Build with Maven

`mvn clean install`

## Code of Conduct

This project adheres to the Contributor Covenant [Code of
Conduct](/CODE_OF_CONDUCT.md). By participating, you are expected to uphold
this code. Please report unacceptable behavior to
code-of-conduct@zeebe.io.
