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
import io.zeebe.containers.ZeebeContainer;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {"zeebe.client.worker.dmn.repository=src/test/resources"})
@Testcontainers
public class WorkflowTest {

  @Container private static final ZeebeContainer ZEEBE_CONTAINER = new ZeebeContainer();

  private static ZeebeClient ZEEBE_CLIENT;

  @BeforeAll
  public static void init() {

    final var gatewayContactPoint = ZEEBE_CONTAINER.getExternalGatewayAddress();
    System.setProperty("zeebe.client.broker.contactPoint", gatewayContactPoint);

    ZEEBE_CLIENT =
        ZeebeClient.newClientBuilder().gatewayAddress(gatewayContactPoint).usePlaintext().build();

    // given
    final BpmnModelInstance workflowDefinition =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask("make-decision")
            .zeebeJobType("DMN")
            .zeebeTaskHeader("decisionRef", "decision")
            .endEvent()
            .done();

    ZEEBE_CLIENT
        .newDeployCommand()
        .addWorkflowModel(workflowDefinition, "process.bpmn")
        .send()
        .join();
  }

  @Test
  public void shouldCompleteWorkflowInstance() {
    // when
    final var workflowInstanceResult =
        ZEEBE_CLIENT
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
