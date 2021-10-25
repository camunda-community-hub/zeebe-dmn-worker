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

import com.google.common.collect.Maps;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.*;

public class RecursiveWatcherService {

  private static final Logger LOG = LoggerFactory.getLogger(RecursiveWatcherService.class);

  private Path rootPath;

  private WatchService watcher;

  private ExecutorService executor;

  private final Consumer<Path> CREATE_CONSUMER;
  private final Consumer<Path> DELETE_CONSUMER;
  private final Consumer<Path> MODIFY_CONSUMER;

  RecursiveWatcherService(Path rootPath,
                          Consumer<Path> CREATE_CONSUMER,
                          Consumer<Path> DELETE_CONSUMER,
                          Consumer<Path> MODIFY_CONSUMER) {
    this.rootPath = rootPath;
    this.CREATE_CONSUMER = CREATE_CONSUMER;
    this.DELETE_CONSUMER = DELETE_CONSUMER;
    this.MODIFY_CONSUMER = MODIFY_CONSUMER;
  }

  public void init() throws IOException {
    watcher = FileSystems.getDefault().newWatchService();
    executor = Executors.newSingleThreadExecutor();
    startRecursiveWatcher();
  }

  public void cleanup() {
    try {
      watcher.close();
      LOG.debug("closing watcher service");
    } catch (IOException e) {
      LOG.error("Error closing watcher service", e);
    }

    executor.shutdown();
  }

  private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }


  private void startRecursiveWatcher() {
    LOG.info("Starting Recursive Watcher");

    final Map<WatchKey, Path> keys = Maps.newHashMap();

    Consumer<Path> register = p -> {
      if (!p.toFile().exists() || !p.toFile().isDirectory()) {
        throw new RuntimeException("folder " + p + " does not exist or is not a directory");
      }
      try {
        Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            LOG.debug("registering {} in watcher service", dir);
                      WatchKey watchKey = dir.register(watcher,
                                new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY},
                                SensitivityWatchEventModifier.HIGH);
                      keys.put(watchKey, dir);
                      return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException e) {
        throw new RuntimeException("Error registering path " + p);
      }
    };

    register.accept(rootPath);

    executor.submit(() -> {
      while (true) {
        final WatchKey key;
        try {
          key = watcher.take();
        } catch (InterruptedException ex) {
          return;
        }

        final Path dir = keys.get(key);
        if (dir == null) {
          LOG.error("WatchKey {} not recognized!", key);
          continue;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> eventKind = event.kind();

          if (eventKind.equals(OVERFLOW)) {
            return;
          }
          WatchEvent<Path> pathEvent = cast(event);
          Path file = pathEvent.context();
          final Path path = dir.resolve(file);
          if (path.toFile().isDirectory()) {
            register.accept(path);
          } else if (eventKind.equals(ENTRY_CREATE)) {
            CREATE_CONSUMER.accept(path);
          } else if (eventKind.equals(ENTRY_MODIFY)) {
            MODIFY_CONSUMER.accept(path);
          } else if (eventKind.equals(ENTRY_DELETE)) {
            DELETE_CONSUMER.accept(path);
          }
        }
        boolean valid = key.reset();
        if (!valid) {
          break;
        }
      }
    });
  }
}
