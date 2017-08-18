# zb-dmn-extension
Zeebe task worker for DMN. It uses the Camunda DMN engine to evaluate decisions. The decisions are read from local directory.

* register for tasks of type 'DMN'
* required task header 'decisionRef' => id of the decision to evaluate
* completes task with payload 'result' which contains the complete decision result

## How to build

Build with Maven

`mvn clean install`

## How to run

Execute the JAR file via

`java -jar target/zeebe-dmn.jar`

## How to configure

You can provide a properties file `application.properties` to configure the Zeebe client and set

* the directory of the DMN repository (default: 'repo')
* the task topic (default: 'default-topic')
