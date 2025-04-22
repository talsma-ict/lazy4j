/*
 * Copyright 2018-2022 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.talsmasoftware.lazy4j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Sjoerd Talsma
 */
public class LazyConcurrencyTest {

    private AtomicInteger counter;
    private Lazy<String> chopper;

    @BeforeEach
    public void setup() {
        counter = new AtomicInteger(0);
        chopper = Lazy.of(() -> {
            counter.incrementAndGet();
            return "Whose motorcycle is this? It's a chopper baby. " +
                    "Whose chopper is this? Zed's. " +
                    "Who's Zed? Zed's dead baby, Zed's dead. **vroaarrr*";
        });
    }

    /**
     * Test 10 threads running 100 processes that each 'get' the lazy value 1000 times
     *
     * @throws InterruptedException if the threadpool doesn't shutdown within a minute.
     */
    @Test
    public void testConcurrentGets() throws InterruptedException {
        final int threads = 10;
        final int processes = 100;
        final int gets = 1000;
        ExecutorService threadpool = Executors.newFixedThreadPool(threads);
        assertThat(counter.get(), is(0));

        final CountDownLatch latch = new CountDownLatch(1);
        final Runnable runnable = () -> {
            try {
                for (int g = 0; g < gets; g++) {
                    latch.await(5, SECONDS);
                    chopper.get();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted waiting for latch!", ie);
            }
        };

        for (int proc = 0; proc < processes; proc++) threadpool.submit(runnable);
        Thread.sleep(SECONDS.toMillis(1));
        latch.countDown();
        threadpool.shutdown();
        if (!threadpool.awaitTermination(1, TimeUnit.MINUTES)) fail("Test timed out.");

        assertThat(counter.get(), is(1));
    }

}
