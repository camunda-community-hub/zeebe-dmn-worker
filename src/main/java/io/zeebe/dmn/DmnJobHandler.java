/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.dmn;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import java.util.Collections;
import java.util.Map;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DmnJobHandler implements JobHandler {

  private static final String DECISION_ID_HEADER = "decisionRef";

  private final DmnRepository repository;
  private final DmnEngine dmnEngine;

  @Autowired
  public DmnJobHandler(DmnRepository repository, DmnEngine dmnEngine) {
    this.repository = repository;
    this.dmnEngine = dmnEngine;
  }

  @Override
  @ZeebeWorker
  public void handle(JobClient client, ActivatedJob job) {

    final DmnDecision decision = findDecisionForTask(job);
    final Map<String, Object> variables = job.getVariablesAsMap();
    final DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, variables);

    client
        .newCompleteCommand(job.getKey())
        .variables(Collections.singletonMap("result", decisionResult))
        .send();
  }

  private DmnDecision findDecisionForTask(ActivatedJob job) {

    final String decisionId = job.getCustomHeaders().get(DECISION_ID_HEADER);
    if (decisionId == null || decisionId.isEmpty()) {
      throw new RuntimeException(String.format("Missing header: '%d'", DECISION_ID_HEADER));
    }

    final DmnDecision decision = repository.findDecisionById(decisionId);
    if (decision == null) {
      throw new RuntimeException(String.format("No decision found with id: '%s'", decisionId));
    }
    return decision;
  }
}
