/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.arrow.flight;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import io.grpc.Metadata;

public class TestCallOptions {

  @Test
  @Ignore
  public void timeoutFires() {
    // Ignored due to CI flakiness
    test((client) -> {
      Instant start = Instant.now();
      Iterator<Result> results = client.doAction(new Action("hang"), CallOptions.timeout(1, TimeUnit.SECONDS));
      try {
        results.next();
        Assert.fail("Call should have failed");
      } catch (RuntimeException e) {
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("deadline exceeded"));
      }
      Instant end = Instant.now();
      Assert.assertTrue("Call took over 1500 ms despite timeout", Duration.between(start, end).toMillis() < 1500);
    });
  }

  @Test
  @Ignore
  public void underTimeout() {
    // Ignored due to CI flakiness
    test((client) -> {
      Instant start = Instant.now();
      // This shouldn't fail and it should complete within the timeout
      Iterator<Result> results = client.doAction(new Action("fast"), CallOptions.timeout(2, TimeUnit.SECONDS));
      Assert.assertArrayEquals(new byte[]{42, 42}, results.next().getBody());
      Instant end = Instant.now();
      Assert.assertTrue("Call took over 2500 ms despite timeout", Duration.between(start, end).toMillis() < 2500);
    });
  }

  @Test
  public void singleProperty() {
    final FlightCallHeaders headers = new FlightCallHeaders();
    headers.insert("key", "value");
    testHeaders(headers);
  }

  @Test
  public void multipleProperties() {
    final FlightCallHeaders headers = new FlightCallHeaders();
    headers.insert("key", "value");
    headers.insert("key2", "value2");
    testHeaders(headers);
  }

  @Test
  public void binaryProperties() {
    final FlightCallHeaders headers = new FlightCallHeaders();
    headers.insert("key-bin", "value".getBytes());
    headers.insert("key3-bin", "ëfßæ".getBytes());
    testHeaders(headers);
  }

  @Test
  public void mixedProperties() {
    final FlightCallHeaders headers = new FlightCallHeaders();
    headers.insert("key", "value");
    headers.insert("key3-bin", "ëfßæ".getBytes());
    testHeaders(headers);
  }

  private void testHeaders(CallHeaders headers) {
    try (
        BufferAllocator a = new RootAllocator(Long.MAX_VALUE);
        HeaderProducer producer = new HeaderProducer();
        FlightServer s =
            FlightTestUtil.getStartedServer((location) -> FlightServer.builder(a, location, producer).build());
        FlightClient client = FlightClient.builder(a, s.getLocation()).build()) {
      // FIXME need to log this one maybe? the checkstyle is kind of strict on this ReturnValueIgnored    
      var hasNext = client.doAction(new Action(""), new HeaderCallOption(headers)).hasNext();

      final CallHeaders incomingHeaders = producer.headers();
      for (String key : headers.keys()) {
        if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
          Assert.assertArrayEquals(headers.getByte(key), incomingHeaders.getByte(key));
        } else {
          Assert.assertEquals(headers.get(key), incomingHeaders.get(key));
        }
      }
    } catch (InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  void test(Consumer<FlightClient> testFn) {
    try (
        BufferAllocator a = new RootAllocator(Long.MAX_VALUE);
        Producer producer = new Producer();
        FlightServer s =
            FlightTestUtil.getStartedServer((location) -> FlightServer.builder(a, location, producer).build());
        FlightClient client = FlightClient.builder(a, s.getLocation()).build()) {
      testFn.accept(client);
    } catch (InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  static class HeaderProducer extends NoOpFlightProducer implements AutoCloseable {
    CallHeaders headers;

    @Override
    public void close() {
    }

    public CallHeaders headers() {
      return headers;
    }

    @Override
    public void doAction(CallContext context, Action action, StreamListener<Result> listener) {
      this.headers = context.getMiddleware(FlightConstants.HEADER_KEY).headers();
      listener.onCompleted();
    }
  }

  static class Producer extends NoOpFlightProducer implements AutoCloseable {

    Producer() {
    }

    @Override
    public void close() {
    }

    @Override
    public void doAction(CallContext context, Action action, StreamListener<Result> listener) {
      switch (action.getType()) {
        case "hang": {
          try {
            Thread.sleep(25000);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          listener.onNext(new Result(new byte[]{}));
          listener.onCompleted();
          return;
        }
        case "fast": {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          listener.onNext(new Result(new byte[]{42, 42}));
          listener.onCompleted();
          return;
        }
        default: {
          throw new UnsupportedOperationException(action.getType());
        }
      }
    }
  }
}
