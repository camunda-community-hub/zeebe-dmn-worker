package io.zeebe.dmn;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.client.TasksClient;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.event.TaskEvent;
import io.zeebe.client.task.TaskHandler;
import io.zeebe.client.task.TaskSubscription;
import org.camunda.bpm.dmn.engine.*;

public class DmnTaskWorker implements TaskHandler
{
    private static final String DECISION_ID_HEADER = "decisionRef";

    private final ZeebeClient client;
    private final String topic;
    private final DmnRepository repository;
    private final DmnEngine dmnEngine;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
            .handler(this)
            .open();
    }

    public void close()
    {
        if (taskSubscription != null && taskSubscription.isOpen())
        {
            taskSubscription.close();
        }
    }

    @Override
    public void handle(TasksClient client, TaskEvent task)
    {
        final DmnDecision decision = findDecisionForTask(task);
        final Map<String, Object> values = getPayloadAsMap(task);

        final DmnDecisionResult decisionResult = dmnEngine.evaluateDecision(decision, values);
        final String resultAsJson = decisionResultAsJson(decisionResult);

        client
            .complete(task)
            .payload(resultAsJson)
            .execute();
    }

    private DmnDecision findDecisionForTask(TaskEvent task)
    {
        final String decisionId = (String) task.getCustomHeaders().get(DECISION_ID_HEADER);
        if (decisionId == null || decisionId.isEmpty())
        {
            throw new RuntimeException(String.format("Missing header: '%d'", DECISION_ID_HEADER));
        }

        final DmnDecision decision = repository.findDecisionById(decisionId);
        if (decision == null)
        {
            throw new RuntimeException(String.format("No decision found with id: '%s'", decisionId));
        }
        return decision;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPayloadAsMap(TaskEvent task)
    {
        final String payload = task.getPayload();

        try
        {
            return objectMapper.readValue(payload, HashMap.class);
        }
        catch (final IOException e)
        {
            throw new RuntimeException(String.format("Failed to parse task payload: %s", payload));
        }
    }

    private String decisionResultAsJson(final DmnDecisionResult decisionResult)
    {
        // need to pack result list into map to be valid payload
        final Map<String, DmnDecisionResult> resultAsMap = Collections.singletonMap("result", decisionResult);

        try
        {
            return objectMapper.writeValueAsString(resultAsMap);
        }
        catch (final IOException e)
        {
            throw new RuntimeException(String.format("Failed to parse decision result: %s", decisionResult));
        }
    }

}
