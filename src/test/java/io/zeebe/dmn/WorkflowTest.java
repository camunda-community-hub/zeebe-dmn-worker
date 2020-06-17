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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.zeebe.client.ZeebeClient;
import io.zeebe.containers.ZeebeBrokerContainer;
import io.zeebe.containers.ZeebePort;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
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
@SpringBootTest(properties = {"zeebe.client.worker.dmn.repository=src/test/resources"})
public class WorkflowTest {

  @ClassRule public static final ZeebeBrokerContainer ZEEBE = new ZeebeBrokerContainer("0.23.2");

  private ZeebeClient client;

  @BeforeClass
  public static void init() {
    final var gatewayContactPoint = ZEEBE.getExternalAddress(ZeebePort.GATEWAY);
    System.setProperty("zeebe.client.broker.contactPoint", gatewayContactPoint);
  }

  @Before
  public void deploy() {
    client =
        ZeebeClient.newClientBuilder()
            .brokerContactPoint(ZEEBE.getExternalAddress(ZeebePort.GATEWAY))
            .usePlaintext()
            .build();

    // given
    final BpmnModelInstance workflowDefinition =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask("make-decision")
            .zeebeJobType("DMN")
            .zeebeTaskHeader("decisionRef", "decision")
            .endEvent()
            .done();

    client.newDeployCommand().addWorkflowModel(workflowDefinition, "process.bpmn").send().join();
  }

  @Test
  public void shouldCompleteWorkflowInstance() {
    // when
    final var workflowInstanceResult =
        client
            .newCreateInstanceCommand()
            .bpmnProcessId("process")
            .latestVersion()
            .variables(Collections.singletonMap("in", "foo"))
            .withResult()
            .send()
            .join();

    // then
    final var expectedResult = List.of(Map.of("out", "yeah!"));

    assertThat(workflowInstanceResult.getVariablesAsMap())
        .contains(entry("result", expectedResult));
  }
}
