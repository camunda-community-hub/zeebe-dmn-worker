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
package io.zeebe;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.events.WorkflowInstanceEvent;
import io.zeebe.dmn.DmnApplication;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.test.ZeebeTestRule;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class WorkflowTest {
  @Rule public ZeebeTestRule testRule = new ZeebeTestRule();

  private ZeebeClient client;

  private DmnApplication application;

  @Before
  public void deploy() {
    client = testRule.getClient();

    final BpmnModelInstance workflowDefinition =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask("make-decision")
            .zeebeTaskType("DMN")
            .zeebeTaskHeader("decisionRef", "decision")
            .endEvent()
            .done();

    client
        .newDeployCommand()
        .addWorkflowModel(workflowDefinition, "process.bpmn")
        .send()
        .join();
  }

  @Before
  public void startApp() {
    final String dmnTestRepo = getClass().getResource("/").getFile();
    final String contactPoint = testRule.getClient().getConfiguration().getBrokerContactPoint();

    application = new DmnApplication(dmnTestRepo, contactPoint);
    application.start();
  }

  @After
  public void cleanUp() {
    application.close();
  }

  @Test
  public void shouldCompleteWorkflowInstance() {
    final WorkflowInstanceEvent workflowInstance =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId("process")
            .latestVersion()
            .payload("{\"in\": \"foo\"}")
            .send()
            .join();

    final List<Map<String, String>> expectedResult =
        Collections.singletonList(Collections.singletonMap("out", "yeah!"));

    ZeebeTestRule.assertThat(workflowInstance)
        .isEnded()
        .hasElementPayload("make-decision", "result", expectedResult);
  }
}
