package io.zeebe.dmn;

import java.util.Optional;
import java.util.stream.Collectors;
import org.camunda.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.camunda.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationListener;
import org.camunda.bpm.dmn.engine.impl.DmnDecisionTableImpl;
import org.camunda.bpm.dmn.engine.impl.DmnDecisionTableRuleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DmnDecisionTableLoggingListener implements DmnDecisionTableEvaluationListener {

  private static final Logger LOG = LoggerFactory.getLogger(DmnDecisionTableLoggingListener.class);

  @Override
  public void notify(
      final DmnDecisionTableEvaluationEvent event) {
    final var decision = event.getDecision();
    final var decisionTable = (DmnDecisionTableImpl) decision.getDecisionLogic();

    final var ruleIds = decisionTable.getRules()
        .stream()
        .map(DmnDecisionTableRuleImpl::getId)
        .collect(Collectors.toList());

    final StringBuilder logBuilder = new StringBuilder();

    logBuilder.append(
        String.format("Evaluated decision table [name: '%s', key: '%s']%n", decision.getName(),
            decision.getKey()));

    event.getInputs().stream().map(
            input -> {
              final var inputValue = input.getValue().getValue();
              return String.format("> Input [label: '%s', value: '%s']%n", input.getName(),
                  inputValue);
            })
        .forEach(logBuilder::append);

    event.getMatchingRules().forEach(rule -> {
          final var ruleIndex = ruleIds.indexOf(rule.getId());

          logBuilder.append(
              String.format("> Matched rule [index: %d]%n", ruleIndex + 1));

          rule.getOutputEntries().values()
              .stream()
              .map(output -> {
                final var outputValue = output.getValue().getValue();
                return String.format(">\t Output [label: '%s', name: '%s', value: '%s']%n",
                    output.getName(),
                    output.getOutputName(),
                    outputValue);
              })
              .forEach(logBuilder::append);
        }
    );

    Optional.ofNullable(event.getCollectResultName())
        .map(collectResultName -> {
          final var collectResultValue = event.getCollectResultValue().getValue();
          return String.format("> Collect result [name: '%s', value: '%s']", collectResultName,
              collectResultValue);
        }).ifPresent(logBuilder::append);

    LOG.info(logBuilder.toString());
  }

}
