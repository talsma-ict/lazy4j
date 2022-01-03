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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static nl.talsmasoftware.lazy4j.Lazy.lazy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

public class MaxRetryTest {
    private static final AtomicLong counter = new AtomicLong(0L);
    private static final Supplier<String> throwingSupplier = () -> {
        counter.incrementAndGet();
        throw new IllegalStateException("Whoops!");
    };

    @BeforeEach
    public void resetCounter() {
        counter.set(0L);
    }

    /**
     * Calls and asserts the thrown exception.
     *
     * @param lazy The lazy value to call
     */
    private static void callAndAssertException(Lazy<String> lazy) {
        try {
            lazy.get();
            fail("LazyEvaluationException expected");
        } catch (LazyEvaluationException expected) {
            assertThat(expected.getCause().getMessage(), is("Whoops!"));
        }
    }

    @Test
    public void testMaxRetries_negative() {
        Lazy<String> lazy = lazy(-1, throwingSupplier);
        for (int i = 0; i < 100; i++) {
            callAndAssertException(lazy);
        }
        assertThat(counter.get(), is(1L));
    }

    @Test
    public void testMaxRetries_zero() {
        Lazy<String> lazy = lazy(0, throwingSupplier);
        for (int i = 0; i < 100; i++) {
            callAndAssertException(lazy);
        }
        assertThat(counter.get(), is(1L));
    }

    @Test
    public void testMaxRetries_one() {
        Lazy<String> lazy = lazy(1, throwingSupplier);
        for (int i = 0; i < 100; i++) {
            callAndAssertException(lazy);
        }
        assertThat(counter.get(), is(1L));
    }

    @Test
    public void testMaxRetries_two() {
        Lazy<String> lazy = lazy(2, throwingSupplier);
        for (int i = 0; i < 100; i++) {
            callAndAssertException(lazy);
        }
        assertThat(counter.get(), is(2L));
    }

    @Test
    public void testMaxRetries_three() {
        Lazy<String> lazy = lazy(3, throwingSupplier);
        for (int i = 0; i < 100; i++) {
            callAndAssertException(lazy);
        }
        assertThat(counter.get(), is(3L));
    }

    @Test
    public void testMaxRetries_fourtyTwo() {
        Lazy<String> lazy = lazy(42, throwingSupplier);
        for (int i = 0; i < 100; i++) {
            callAndAssertException(lazy);
        }
        assertThat(counter.get(), is(42L));
    }

    @Test
    public void testMaxRetries_unlimited() {
        Lazy<String> lazy = lazy(Integer.MAX_VALUE, throwingSupplier);
        for (int i = 0; i < 100; i++) {
            callAndAssertException(lazy);
        }
        assertThat(counter.get(), is(100L));
    }
}
