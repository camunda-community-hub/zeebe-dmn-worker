package io.zeebe.dmn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DmnRepository
{
    private final static Logger LOG = LoggerFactory.getLogger(DmnRepository.class);

    private final String directory;
    private final DmnEngine dmnEngine;

    public DmnRepository(String directory, DmnEngine dmnEngine)
    {
        this.directory = directory;
        this.dmnEngine = dmnEngine;
    }

    private final Map<String, DmnDecision> decisionsById = new HashMap<>();

    public DmnDecision findDecisionById(String decisionId)
    {
        if (!decisionsById.containsKey(decisionId))
        {
            scanDirectory();
        }

        return decisionsById.get(decisionId);
    }

    private void scanDirectory()
    {
        LOG.debug("Scan directory: {}", directory);

        try
        {
            Files.walk(Paths.get(directory)).filter(p -> p.getFileName().toString().endsWith(".dmn")).forEach(p ->
            {
                try
                {
                    final DmnModelInstance dmnModel = Dmn.readModelFromFile(p.toFile());
                    dmnEngine.parseDecisions(dmnModel).forEach(decision ->
                    {
                        LOG.debug("Found decision with id '{}' in file: {}", decision.getKey(), p.getFileName());

                        decisionsById.put(decision.getKey(), decision);
                    });
                }
                catch (Throwable t)
                {
                    LOG.warn("Failed to parse decision: {}", p.getFileName(), t);
                }
            });
        }
        catch (IOException e)
        {
            LOG.warn("Fail to scan directory: {}", directory, e);
        }
    }

}
