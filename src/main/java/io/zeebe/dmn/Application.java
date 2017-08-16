package io.zeebe.dmn;

import java.util.Properties;
import java.util.Scanner;

import io.zeebe.client.ClientProperties;
import io.zeebe.client.ZeebeClient;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application
{
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private static final String TOPIC = "default-topic";

    public static void main(String[] args)
    {
        final Properties clientProperties = new Properties();
        clientProperties.put(ClientProperties.BROKER_CONTACTPOINT, "127.0.0.1:51015");

        final ZeebeClient client = ZeebeClient.create(clientProperties);

        client.connect();
        LOG.debug("Connected.");

        // TODO config repository
        final DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
        final DmnRepository repository = new DmnRepository("repo", dmnEngine);
        final DmnTaskWorker taskWorker = new DmnTaskWorker(client, TOPIC, repository, dmnEngine);

        waitUntilClose();

        taskWorker.close();

        client.close();
        LOG.debug("Closed.");
    }

    private static void waitUntilClose()
    {
        try (Scanner scanner = new Scanner(System.in))
        {
            while (scanner.hasNextLine())
            {
                final String nextLine = scanner.nextLine();
                if (nextLine.contains("close"))
                {
                    return;
                }
            }
        }
    }

}
