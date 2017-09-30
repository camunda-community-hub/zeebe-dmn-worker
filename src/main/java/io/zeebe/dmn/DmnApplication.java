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

import java.util.Properties;

import io.zeebe.client.ZeebeClient;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DmnApplication
{
    private static final Logger LOG = LoggerFactory.getLogger(DmnApplication.class);

    private final Properties clientProperties = new Properties();

    private final String repoDir;
    private final String topic;

    private DmnTaskWorker taskWorker;
    private ZeebeClient client;

    public DmnApplication(String repoDir, String topic)
    {
        this.repoDir = repoDir;
        this.topic = topic;
    }

    public void start()
    {
        client = ZeebeClient.create(clientProperties);

        LOG.debug("Connected.");

        taskWorker = buildTaskWorker(client);
        taskWorker.open();

        LOG.debug("Started.");
    }

    private DmnTaskWorker buildTaskWorker(final ZeebeClient client)
    {
        final DmnEngine dmnEngine = buildDmnEngine();

        final DmnRepository repository = new DmnRepository(repoDir, dmnEngine);

        return new DmnTaskWorker(client, topic, repository, dmnEngine);
    }

    private DmnEngine buildDmnEngine()
    {
        final DefaultDmnEngineConfiguration config = (DefaultDmnEngineConfiguration) DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
        config.setDefaultInputEntryExpressionLanguage("feel-scala-unary-tests");
        config.setDefaultOutputEntryExpressionLanguage("feel-scala");
        config.setDefaultInputExpressionExpressionLanguage("feel-scala");
        config.setDefaultLiteralExpressionLanguage("feel-scala");
        return config.buildEngine();
    }

    public void close()
    {
        taskWorker.close();
        client.close();

        LOG.debug("Closed.");
    }

}
