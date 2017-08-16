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

import static io.zeebe.fixtures.ZeebeTestRule.DEFAULT_TOPIC;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.event.WorkflowInstanceEvent;
import io.zeebe.dmn.DmnRepository;
import io.zeebe.dmn.DmnTaskWorker;
import io.zeebe.fixtures.ZeebeTestRule;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.instance.WorkflowDefinition;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.junit.*;

public class WorkflowTest
{
    @Rule
    public ZeebeTestRule testRule = new ZeebeTestRule();

    private ZeebeClient client;
    private DmnTaskWorker taskWorker;

    @Before
    public void deploy()
    {
        client = testRule.getClient();

        final WorkflowDefinition workflowDefinition = Bpmn.createExecutableWorkflow("process")
                .startEvent()
                .sequenceFlow()
                .serviceTask("make-decision")
                    .taskType("DMN")
                    .taskRetries(3)
                    .taskHeader("decisionRef", "decision")
                    .output("$.result", "$.result")
                    .done()
                .sequenceFlow()
                .endEvent()
                .done();

        final String workflowAsString = Bpmn.convertToString(workflowDefinition);

        client.workflows().deploy(DEFAULT_TOPIC)
                .resourceStringUtf8(workflowAsString)
                .execute();

        final DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
        final String dmnDirectory = getClass().getResource("/").getFile();
        final DmnRepository repository = new DmnRepository(dmnDirectory, dmnEngine);
        taskWorker = new DmnTaskWorker(client, DEFAULT_TOPIC, repository, dmnEngine);
        taskWorker.open();
    }

    @After
    public void cleanUp()
    {
        taskWorker.close();
    }

    @Test
    public void shouldCompleteWorkflowInstance()
    {
        final WorkflowInstanceEvent workflowInstance = client.workflows().create(DEFAULT_TOPIC)
            .bpmnProcessId("process")
            .latestVersion()
            .payload("{\"in\": \"foo\"}")
            .execute();

        testRule.waitUntilWorklowInstanceCompleted(workflowInstance.getWorkflowInstanceKey());

        testRule.printWorkflowInstanceEvents(workflowInstance.getWorkflowInstanceKey());
    }

}
