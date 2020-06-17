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
    return config.buildEngine();
  }
}
