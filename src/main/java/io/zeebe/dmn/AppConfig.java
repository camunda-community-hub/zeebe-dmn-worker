package io.zeebe.dmn;

import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

  @Bean
  public DmnEngine createDmnEngine() {

    final DefaultDmnEngineConfiguration config =
        (DefaultDmnEngineConfiguration)
            DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
    config.setDefaultInputEntryExpressionLanguage("feel-scala-unary-tests");
    config.setDefaultOutputEntryExpressionLanguage("feel-scala");
    config.setDefaultInputExpressionExpressionLanguage("feel-scala");
    config.setDefaultLiteralExpressionLanguage("feel-scala");
    return config.buildEngine();
  }
}
