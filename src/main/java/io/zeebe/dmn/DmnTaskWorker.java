package io.zeebe.dmn;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.task.TaskSubscription;
import org.camunda.bpm.dmn.engine.*;

public class DmnTaskWorker
{
    private static final String DECISION_REF = "decisionRef";

    private final ZeebeClient client;
    private final String topic;
    private final DmnRepository repository;
    private final DmnEngine dmnEngine;

    private TaskSubscription taskSubscription;

    public DmnTaskWorker(ZeebeClient client, String topic, DmnRepository repository, DmnEngine dmnEngine)
    {
        this.client = client;
        this.topic = topic;
        this.repository = repository;
        this.dmnEngine = dmnEngine;
    }

    public void open()
    {
        taskSubscription = client.tasks().newTaskSubscription(topic)
            .taskType("DMN")
            .lockOwner("camunda-dmn")
            .lockTime(Duration.ofSeconds(10))
            .handler((client, task) ->
            {
                final String decisionId = (String) task.getCustomHeaders().get(DECISION_REF);
                if (decisionId == null || decisionId.isEmpty())
                {
                    throw new RuntimeException(String.format("Missing header: '%d'", DECISION_REF));
                }

                final DmnDecision decision = repository.findDecisionById(decisionId);
                if (decision == null)
                {
                    throw new RuntimeException(String.format("No decision found with id: '%s'", decisionId));
                }

                final ObjectMapper objectMapper = new ObjectMapper();

                Map<String, Object> values;
                try
                {
                    values = objectMapper.readValue(task.getPayload(), HashMap.class);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(String.format("Failed to parse task payload: %s", task.getPayload()));
                }

                try
                {
                    final DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, values);
                    final Map<String, DmnDecisionResult> resultAsMap = Collections.singletonMap("result", decisionResult);
                    final String resultAsJson = objectMapper.writeValueAsString(resultAsMap);

                    client
                        .complete(task)
                        .payload(resultAsJson)
                        .execute();
                }
                catch (Throwable t)
                {
                    throw new RuntimeException(String.format("Failed to evaluate decision with id: %s", decisionId), t);
                }
            })
            .open();
    }

    public void close()
    {
        if (taskSubscription != null && taskSubscription.isOpen())
        {
            taskSubscription.close();
        }
    }

}
