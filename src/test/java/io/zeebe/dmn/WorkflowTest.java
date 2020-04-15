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

import io.zeebe.client.api.response.WorkflowInstanceEvent;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.test.ZeebeTestRule;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"zeebe.worker.dmn.repository=src/test/resources"})
public class WorkflowTest {

  @ClassRule public static final ZeebeTestRule TEST_RULE = new ZeebeTestRule();

  @BeforeClass
  public static void init() {
    System.setProperty(
        "zeebe.client.broker.contactPoint",
        TEST_RULE.getClient().getConfiguration().getBrokerContactPoint());
  }

  @Before
  public void deploy() {
    final BpmnModelInstance workflowDefinition =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask("make-decision")
            .zeebeJobType("DMN")
            .zeebeTaskHeader("decisionRef", "decision")
            .endEvent()
            .done();

    TEST_RULE
        .getClient()
        .newDeployCommand()
        .addWorkflowModel(workflowDefinition, "process.bpmn")
        .send()
        .join();
  }

  @Test
  public void shouldCompleteWorkflowInstance() {
    final WorkflowInstanceEvent workflowInstance =
        TEST_RULE
            .getClient()
            .newCreateInstanceCommand()
            .bpmnProcessId("process")
            .latestVersion()
            .variables(Collections.singletonMap("in", "foo"))
            .send()
            .join();

    final List<Map<String, String>> expectedResult =
        Collections.singletonList(Collections.singletonMap("out", "yeah!"));

    ZeebeTestRule.assertThat(workflowInstance).isEnded().hasVariable("result", expectedResult);
  }
}
