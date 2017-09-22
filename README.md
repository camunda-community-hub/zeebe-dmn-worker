# zb-dmn-extension
Zeebe task worker for DMN. It uses the Camunda DMN engine to evaluate decisions. The decisions are read from local directory.

* register for tasks of type 'DMN'
* required task header 'decisionRef' => id of the decision to evaluate
* completes task with payload 'result' which contains the complete decision result

```
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

## How to build

Build with Maven

`mvn clean install`

## How to run

Execute the JAR file via

`java -jar zeebe-dmn.jar`

## How to configure

You can provide a properties file `application.properties` to configure the Zeebe client and the DMN task worker.

```
# DMN task worker
zeebe.dmn.repo=dmn-repo                             # default: repo
zeebe.dmn.topic=default-topic                       # => default
# Zeebe Client
zeebe.client.broker.contactPoint=127.0.0.1:51015    # => default
```
