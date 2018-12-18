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

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class StandaloneDmnApplication {

  public static final String ENV_CONTACT_POINT = "zeebe.client.broker.contactPoint";
  public static final String ENV_REPOSITORY = "dmn.repo";

  private static final String DEFAULT_REPO_DIR = "dmn-repo";
  private static final String DEFAULT_CONTACT_POINT = "127.0.0.1:26500";

  public static void main(String[] args) {
    final String repoDir =
        Optional.ofNullable(System.getenv(ENV_REPOSITORY)).orElse(DEFAULT_REPO_DIR);
    final String contractPoint =
        Optional.ofNullable(System.getenv(ENV_CONTACT_POINT)).orElse(DEFAULT_CONTACT_POINT);

    final DmnApplication application = new DmnApplication(repoDir, contractPoint);
    application.start();

    try {
      new CountDownLatch(1).await();
    } catch (InterruptedException e) {
    }
  }
}
