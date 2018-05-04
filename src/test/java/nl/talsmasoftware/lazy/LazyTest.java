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
import java.util.function.Function;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

public class LazyTest {
    private AtomicInteger counter;
    private Lazy<String> mayonaise, exception;

    @Before
    public void setUp() {
        counter = new AtomicInteger(0);
        mayonaise = Lazy.lazy(counting(() -> "I've seen them do it man, they f*n drown them in that shit!"));
        exception = Lazy.lazy(counting(() -> {
            throw new IllegalStateException("Whoops!");
        }));
    }

    /**
     * Resolve the lazy object, swallowing any potential LazyEvaluationExceptions.
     * <p>
     * Note that no distinction can be made from lazy null values and lazy exceptions.
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
    public void testLazyNull() {
        try {
            Lazy.lazy(null);
            fail("Exception expected");
        } catch (NullPointerException expected) {
            assertThat(expected, hasToString(containsString("Lazy function is <null>")));
        }
    }

    @Test
    public void testLazy() {
        assertThat(counter.get(), is(0));
        for (int i = 0; i < 100; i++) {
            assertThat(mayonaise.get(), equalTo("I've seen them do it man, they f*n drown them in that shit!"));
        }
        assertThat(counter.get(), is(1));
    }

    /**
     * Test that even when the result is {@code null}, the supplier gets called only once.
     */
    @Test
    public void testLazyNullValue() {
        final Lazy<String> nothing = Lazy.lazy(counting(() -> null));
        assertThat(counter.get(), is(0));

        for (int i = 0; i < 100; i++) assertThat(nothing.get(), is(nullValue()));
        assertThat(counter.get(), is(1));
    }

    /**
     * Test that even when the result is an exception, the supplier gets called only once.
     */
    @Test
    public void testLazyException() {
        for (int i = 0; i < 100; i++)
            try {
                exception.get();
                fail("Exception expected");
            } catch (LazyEvaluationException expected) {
                assertThat(expected.getCause(), is(instanceOf(IllegalStateException.class)));
                assertThat(expected.getCause().getMessage(), equalTo("Whoops!"));
            }
        assertThat(counter.get(), is(1));
    }

    @Test
    public void testMap() {
        AtomicInteger mapCounter = new AtomicInteger(0);
        Lazy<String> reverse = mayonaise.map(counting(mapCounter, s -> new StringBuffer(s).reverse().toString()));
        assertThat(counter.get(), is(0));
        assertThat(mapCounter.get(), is(0));

        for (int i = 0; i < 100; i++) {
            assertThat(reverse.get(), equalTo("!tihs taht ni meht nword n*f yeht ,nam ti od meht nees ev'I"));
        }
        assertThat(counter.get(), is(1));
        assertThat(mapCounter.get(), is(1));
    }

    @Test
    public void testFlatMap() {
        AtomicInteger mapCounter = new AtomicInteger(0);
        AtomicInteger flatteningCounter = new AtomicInteger(0);
        Lazy<String> reverse = mayonaise.flatMap(counting(mapCounter,
                s -> Lazy.lazy(counting(flatteningCounter, (Supplier<String>) s::toUpperCase))));
        assertThat(counter.get(), is(0));
        assertThat(mapCounter.get(), is(0));
        assertThat(flatteningCounter.get(), is(0));

        for (int i = 0; i < 100; i++) {
            assertThat(reverse.get(), equalTo("I'VE SEEN THEM DO IT MAN, THEY F*N DROWN THEM IN THAT SHIT!"));
        }
        assertThat(counter.get(), is(1));
        assertThat(mapCounter.get(), is(1));
        assertThat(flatteningCounter.get(), is(1));
    }

    @Test
    public void testToString_unresolved() {
        assertThat(mayonaise, hasToString(equalTo("Lazy[not yet resolved]")));
        assertThat(counter.get(), is(0));
    }

    @Test
    public void testToString_unresolved_exception() {
        assertThat(exception, hasToString(equalTo("Lazy[not yet resolved]")));
        assertThat(counter.get(), is(0));
    }

    @Test
    public void testToString_resolved() {
        resolve(mayonaise);
        assertThat(mayonaise, hasToString(equalTo("Lazy[I've seen them do it man, they f*n drown them in that shit!]")));
        assertThat(counter.get(), is(1));
    }

    @Test
    public void testToString_resolved_exception() {
        resolve(exception);
        assertThat(exception, hasToString(equalTo("Lazy[threw exception]")));
        assertThat(counter.get(), is(1));
    }

    private <T> Supplier<T> counting(Supplier<T> supplier) {
        return counting(counter, supplier);
    }

    private static <T> Supplier<T> counting(AtomicInteger counter, Supplier<T> supplier) {
        return () -> {
            counter.incrementAndGet();
            return supplier.get();
        };
    }

    private static <T, U> Function<T, U> counting(AtomicInteger counter, Function<T, U> function) {
        return input -> {
            counter.incrementAndGet();
            return function.apply(input);
        };
    }
}
