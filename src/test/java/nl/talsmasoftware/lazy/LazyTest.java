/*
 * Copyright 2018 Talsma ICT
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
package nl.talsmasoftware.lazy;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

public class LazyTest {
    private Lazy<String> mayonaise, exception;

    @Before
    public void setUp() {
        mayonaise = Lazy.evaluate(() -> "I've seen them do it man, they f*n drown them in that shit!");
        exception = Lazy.evaluate(() -> {
            throw new IllegalStateException("Whoops!");
        });
    }

    /**
     * Resolve the lazy object, swallowing any potential LazyEvaluationExceptions.
     *
     * @param lazy The lazy object to be resolved.
     */
    private static <T> Optional<T> resolve(Lazy<T> lazy) {
        try {
            return Optional.ofNullable(lazy.get());
        } catch (LazyEvaluationException expected) {
            return Optional.empty();
        }
    }

    /**
     * Assert that we get a decent exception message for a {@code null} supplier.
     */
    @Test
    public void testLazyEvaluateNull() {
        try {
            Lazy.evaluate(null);
            fail("Exception expected");
        } catch (NullPointerException expected) {
            assertThat(expected, hasToString(containsString("Lazy supplier is <null>")));
        }
    }

    @Test
    public void testLazyEvaluate() {
        final AtomicInteger count = new AtomicInteger(0);
        final Lazy<String> chopper = Lazy.evaluate(() -> {
            count.incrementAndGet();
            return "Whose motorcycle is that? It's a chopper baby! Whose chopper is that?";
        });
        for (int i = 0; i < 100; i++) {
            assertThat(chopper.get(), equalTo("Whose motorcycle is that? It's a chopper baby! Whose chopper is that?"));
        }
        assertThat(count.get(), is(1));
    }

    /**
     * Test that even when the result is {@code null}, the supplier gets called only once.
     */
    @Test
    public void testLazyNullValue() {
        final AtomicInteger count = new AtomicInteger(0);
        final Lazy<String> nothing = Lazy.evaluate(() -> {
            count.incrementAndGet();
            return null;
        });
        for (int i = 0; i < 100; i++) assertThat(nothing.get(), is(nullValue()));
        assertThat(count.get(), is(1));
    }

    /**
     * Test that even when the result is an exception, the supplier gets called only once.
     */
    @Test
    public void testLazyException() {
        final AtomicInteger count = new AtomicInteger(0);
        final Lazy<String> whoops = Lazy.evaluate(() -> {
            count.incrementAndGet();
            throw new IllegalStateException("Whoops!");
        });
        for (int i = 0; i < 100; i++)
            try {
                whoops.get();
                fail("Exception expected");
            } catch (LazyEvaluationException expected) {
                assertThat(expected.getCause(), is(instanceOf(IllegalStateException.class)));
                assertThat(expected.getCause().getMessage(), equalTo("Whoops!"));
            }
        assertThat(count.get(), is(1));
    }

    @Test
    public void testToString_unresolved() {
        assertThat(mayonaise, hasToString(equalTo("Lazy[not yet resolved]")));
    }

    @Test
    public void testToString_unresolved_exception() {
        assertThat(exception, hasToString(equalTo("Lazy[not yet resolved]")));
    }

    @Test
    public void testToString_resolved() {
        resolve(mayonaise);
        assertThat(mayonaise, hasToString(equalTo("Lazy[I've seen them do it man, they f*n drown them in that shit!]")));
    }

    @Test
    public void testToString_resolved_exception() {
        resolve(exception);
        assertThat(exception, hasToString(equalTo("Lazy[threw exception]")));
    }
}
