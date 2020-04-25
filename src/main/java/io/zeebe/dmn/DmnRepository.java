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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.PostConstruct;
import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DmnRepository {
  private static final Logger LOG = LoggerFactory.getLogger(DmnRepository.class);

  @Value("${zeebe.client.worker.dmn.repository}")
  private String dmnRepositoryFolder;

  private final DmnEngine dmnEngine;

  private final Map<String, DmnDecision> decisionsById = new HashMap<>();

  @Autowired
  public DmnRepository(DmnEngine dmnEngine) {
    this.dmnEngine = dmnEngine;
  }

  public DmnDecision findDecisionById(String decisionId) {
    if (!decisionsById.containsKey(decisionId)) {
      scanDirectory();
    }

    return decisionsById.get(decisionId);
  }

  @PostConstruct
  public void scanDirectory() {

    final Path repositoryPath = Paths.get(dmnRepositoryFolder);

    LOG.info("Scan directory: {}", repositoryPath.toAbsolutePath());

    try {
      Files.walk(repositoryPath).filter(isDmnFile()).forEach(this::readDmnFile);
    } catch (IOException e) {
      LOG.error("Fail to scan directory: {}", repositoryPath.toAbsolutePath(), e);
    }
  }

  private void readDmnFile(Path dmnFile) {
    final String fileName = dmnFile.getFileName().toString();

    try {
      final DmnModelInstance dmnModel = Dmn.readModelFromFile(dmnFile.toFile());

      dmnEngine
          .parseDecisions(dmnModel)
          .forEach(
              decision -> {
                LOG.info(
                    "Found decision with id '{}' in file: {}",
                    decision.getKey(),
                    dmnFile.toAbsolutePath());

                decisionsById.put(decision.getKey(), decision);
              });
    } catch (Throwable t) {
      LOG.warn("Failed to parse decision: {}", fileName, t);
    }
  }

  private Predicate<Path> isDmnFile() {
    return p -> p.getFileName().toString().toLowerCase().endsWith(".dmn");
  }
}
