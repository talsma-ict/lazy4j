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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;

public class LazyTest {
    private AtomicInteger counter;
    private Lazy<String> mayonaise, exception;

    @BeforeEach
    public void setUp() {
        counter = new AtomicInteger(0);
        mayonaise = Lazy.of(counting(() -> "I've seen them do it man, they f*n drown them in that shit!"));
        exception = Lazy.of(counting(() -> {
            throw new IllegalStateException("Whoops!");
        }));
    }

    /**
     * Resolve the lazy object, swallowing any potential LazyEvaluationExceptions.
     *
     * <p>
     * Note that no distinction can be made from lazy null values and lazy exceptions.
     *
     * @param lazy The lazy object to be resolved.
     */
    private static <T> Optional<T> resolve(Lazy<T> lazy) {
        try {
            return Optional.ofNullable(lazy.get());
        } catch (RuntimeException expected) {
            return Optional.empty();
        }
    }

    /**
     * Assert that we get a decent exception message for a {@code null} supplier.
     */
    @Test
    public void testLazyNull() {
        try {
            Lazy.of(null);
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
        final Lazy<String> nothing = Lazy.of(counting(() -> null));
        assertThat(counter.get(), is(0));

        for (int i = 0; i < 100; i++) assertThat(nothing.get(), is(nullValue()));
        assertThat(counter.get(), is(1));
    }

    /**
     * Test when the result is an exception, the supplier gets called again to retry obtaining a lazy value.
     */
    @Test
    public void testLazyException() {
        for (int i = 0; i < 100; i++)
            try {
                exception.get();
                fail("Exception expected");
            } catch (RuntimeException expected) {
                assertThat(expected, is(instanceOf(IllegalStateException.class)));
                assertThat(expected.getMessage(), equalTo("Whoops!"));
            }
        assertThat(counter.get(), is(100));
    }

    @Test
    public void testIsAvailable() {
        assertThat(mayonaise.isAvailable(), is(false));
        assertThat(counter.get(), is(0));
        resolve(mayonaise);
        assertThat(mayonaise.isAvailable(), is(true));
        assertThat(mayonaise.isAvailable(), is(true));
        assertThat(counter.get(), is(1));
    }

    @Test
    public void testIsAvailable_exception() {
        assertThat(exception.isAvailable(), is(false));
        assertThat(counter.get(), is(0));
        resolve(exception);
        assertThat(exception.isAvailable(), is(false));
        assertThat(exception.isAvailable(), is(false));
        assertThat(counter.get(), is(1));
    }

    @Test
    public void testGetIfAvailable() {
        assertThat(mayonaise.getIfAvailable(), is(Optional.empty()));
        assertThat(counter.get(), is(0));
        resolve(mayonaise);
        assertThat(mayonaise.getIfAvailable(), is(Optional.of("I've seen them do it man, they f*n drown them in that shit!")));
        assertThat(mayonaise.getIfAvailable(), is(Optional.of("I've seen them do it man, they f*n drown them in that shit!")));
        assertThat(counter.get(), is(1));
    }

    @Test
    public void testIfAvailable() {
        AtomicInteger availableCounter = new AtomicInteger(0);
        mayonaise.ifAvailable(value -> availableCounter.incrementAndGet());
        assertThat(counter.get(), is(0));
        assertThat(availableCounter.get(), is(0));

        resolve(mayonaise);
        mayonaise.ifAvailable(value -> availableCounter.incrementAndGet());
        assertThat(counter.get(), is(1));
        assertThat(availableCounter.get(), is(1));

        mayonaise.ifAvailable(value -> availableCounter.incrementAndGet());
        assertThat(counter.get(), is(1));
        assertThat(availableCounter.get(), is(2));
    }

    @Test
    public void testIfAvailable_exception() {
        AtomicInteger availableCounter = new AtomicInteger(0);
        exception.ifAvailable(value -> availableCounter.incrementAndGet());
        assertThat(counter.get(), is(0));
        assertThat(availableCounter.get(), is(0));

        resolve(exception);
        exception.ifAvailable(value -> availableCounter.incrementAndGet());
        exception.ifAvailable(value -> availableCounter.incrementAndGet());
        assertThat(counter.get(), is(1));
        assertThat(availableCounter.get(), is(0));
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
                s -> Lazy.of(counting(flatteningCounter, (Supplier<String>) s::toUpperCase))));
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
    public void testToString_resolved_null() {
        Lazy<Object> lazyNull = Lazy.of(() -> null);
        assertThat(lazyNull.get(), is(nullValue()));
        assertThat(lazyNull, hasToString(equalTo("Lazy[null]")));
    }

    @Test
    public void testToString_unresolved_due_to_exception() {
        resolve(exception);
        assertThat(exception, hasToString(equalTo("Lazy[not yet resolved]")));
        assertThat(counter.get(), is(1));
    }

    @Test
    @SuppressWarnings("deprecation") // We test the deprecated method
    public void testDeprecatedFactoryMethod() {
        Lazy<String> lazyString = Lazy.lazy(mayonaise);
        assertThat(lazyString.isAvailable(), is(false));
        assertThat(mayonaise.isAvailable(), is(false));

        assertThat(lazyString.get(), is("I've seen them do it man, they f*n drown them in that shit!"));
        assertThat(lazyString.get(), is("I've seen them do it man, they f*n drown them in that shit!"));

        assertThat(lazyString.isAvailable(), is(true));
        assertThat(mayonaise.isAvailable(), is(true));
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
