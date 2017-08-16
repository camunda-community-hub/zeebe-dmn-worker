# zb-dmn-extension
Zeebe task worker for DMN. It uses the Camunda DMN engine to evaluate decisions. The decisions are read from a directory (default 'repo').

* register for tasks of type 'DMN'
* task requires a header 'decisionRef' which holds the id of the decision to evaluate
* task is completed with payload 'result' which contains the decision result
