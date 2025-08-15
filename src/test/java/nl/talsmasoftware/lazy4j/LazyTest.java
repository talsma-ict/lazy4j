/*
 * Copyright 2018-2025 Talsma ICT
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LazyTest {
    AtomicInteger counter;
    Lazy<String> mayonaise, exception, eager;

    @BeforeEach
    void setUp() {
        counter = new AtomicInteger(0);
        mayonaise = Lazy.of(counting(() -> "I've seen them do it man, they f*n drown them in that shit!"));
        exception = Lazy.of(counting(() -> {
            throw new IllegalStateException("Whoops!");
        }));
        eager = Lazy.eager("I'm eager!");
    }

    /**
     * Resolve the lazy object, swallowing any potential LazyEvaluationExceptions.
     *
     * <p>
     * Note that no distinction can be made from lazy null values and lazy exceptions.
     *
     * @param lazy The lazy object to be resolved.
     */
    static <T> Optional<T> resolve(Lazy<T> lazy) {
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
    void testLazyNull() {
        assertThatThrownBy(() -> Lazy.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Lazy function is <null>");
    }

    @Test
    void testLazy() {
        assertThat(counter.get()).isZero();
        for (int i = 0; i < 100; i++) {
            assertThat(mayonaise.get()).isEqualTo("I've seen them do it man, they f*n drown them in that shit!");
        }
        assertThat(counter.get()).isOne();
    }

    @Test
    void testEager() {
        final String value = "The quick brown fox jumps over the lazy dog";
        Lazy<String> subject = Lazy.eager(value);
        assertThat(subject.isAvailable()).isTrue();
        assertThat(subject.getIfAvailable()).contains(value);

        AtomicBoolean called = new AtomicBoolean(false);
        subject.ifAvailable(v -> called.set(true));
        assertThat(called.get()).as("Callback to ifAvailable called?").isTrue();

        assertThat(subject.get()).isEqualTo(value);
    }

    /**
     * Test that even when the result is {@code null}, the supplier gets called only once.
     */
    @Test
    void testLazyNullValue() {
        final Lazy<String> nothing = Lazy.of(counting(() -> null));
        assertThat(counter.get()).isZero();

        for (int i = 0; i < 100; i++) {
            assertThat(nothing.get()).isNull();
        }
        assertThat(counter.get()).isOne();
    }

    @Test
    void lazyOfLazy() {
        assertThat(Lazy.of(mayonaise)).isSameAs(mayonaise);
        assertThat(Lazy.of(exception)).isSameAs(exception);
        assertThat(Lazy.of(eager)).isSameAs(eager);
    }

    /**
     * Test when the result is an exception, the supplier gets called again to retry getting a lazy value.
     */
    @Test
    void testLazyException() {
        for (int i = 0; i < 100; i++) {
            assertThatThrownBy(exception::get)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Whoops!");
        }
        assertThat(counter.get()).isEqualTo(100);
    }

    @Test
    void testIsAvailable() {
        assertThat(mayonaise.isAvailable()).isFalse();
        assertThat(counter.get()).isZero();
        resolve(mayonaise);
        assertThat(mayonaise.isAvailable()).isTrue();
        assertThat(mayonaise.isAvailable()).isTrue();
        assertThat(counter.get()).isOne();
    }

    @Test
    void testIsAvailable_exception() {
        assertThat(exception.isAvailable()).isFalse();
        assertThat(counter.get()).isZero();
        resolve(exception);
        assertThat(exception.isAvailable()).isFalse();
        assertThat(exception.isAvailable()).isFalse();
        assertThat(counter.get()).isOne();
    }

    @Test
    void testGetIfAvailable() {
        assertThat(mayonaise.getIfAvailable()).isEmpty();
        assertThat(counter.get()).isZero();
        resolve(mayonaise);
        assertThat(mayonaise.getIfAvailable()).contains("I've seen them do it man, they f*n drown them in that shit!");
        assertThat(mayonaise.getIfAvailable()).contains("I've seen them do it man, they f*n drown them in that shit!");
        assertThat(counter.get()).isOne();
    }

    @Test
    void testIfAvailable() {
        AtomicInteger callbackCounter = new AtomicInteger(0);
        mayonaise.ifAvailable(value -> callbackCounter.incrementAndGet());
        assertThat(counter.get()).isZero();
        assertThat(callbackCounter.get()).isZero();

        resolve(mayonaise);
        mayonaise.ifAvailable(value -> callbackCounter.incrementAndGet());
        assertThat(counter.get()).isOne();
        assertThat(callbackCounter.get()).isOne();

        mayonaise.ifAvailable(value -> callbackCounter.incrementAndGet());
        assertThat(counter.get()).isOne();
        assertThat(callbackCounter.get()).isEqualTo(2);
    }

    @Test
    void testIfAvailable_exception() {
        AtomicInteger availableCounter = new AtomicInteger(0);
        exception.ifAvailable(value -> availableCounter.incrementAndGet());
        assertThat(counter.get()).isZero();
        assertThat(availableCounter.get()).isZero();

        resolve(exception);
        exception.ifAvailable(value -> availableCounter.incrementAndGet());
        exception.ifAvailable(value -> availableCounter.incrementAndGet());
        assertThat(counter.get()).isOne();
        assertThat(availableCounter.get()).isZero();
    }

    @Test
    void testMap() {
        AtomicInteger mapCounter = new AtomicInteger(0);
        Lazy<String> reverse = mayonaise.map(counting(mapCounter, s -> new StringBuffer(s).reverse().toString()));
        assertThat(counter.get()).isZero();
        assertThat(mapCounter.get()).isZero();
        assertThat(mayonaise.isAvailable()).isFalse();
        assertThat(reverse.isAvailable()).isFalse();

        for (int i = 0; i < 100; i++) {
            assertThat(reverse.get()).isEqualTo("!tihs taht ni meht nword n*f yeht ,nam ti od meht nees ev'I");
        }
        assertThat(mayonaise.isAvailable()).isTrue();
        assertThat(reverse.isAvailable()).isTrue();
        assertThat(counter.get()).isOne();
        assertThat(mapCounter.get()).isOne();
    }

    @Test
    void testFlatMap() {
        AtomicInteger mapCounter = new AtomicInteger(0);
        AtomicInteger flatteningCounter = new AtomicInteger(0);
        Lazy<String> reverse = mayonaise.flatMap(counting(mapCounter,
                s -> Lazy.of(counting(flatteningCounter, (Supplier<String>) s::toUpperCase))));
        assertThat(counter.get()).isZero();
        assertThat(mapCounter.get()).isZero();
        assertThat(flatteningCounter.get()).isZero();

        for (int i = 0; i < 100; i++) {
            assertThat(reverse.get()).isEqualTo("I'VE SEEN THEM DO IT MAN, THEY F*N DROWN THEM IN THAT SHIT!");
        }
        assertThat(counter.get()).isOne();
        assertThat(mapCounter.get()).isOne();
        assertThat(flatteningCounter.get()).isOne();
    }

    @Test
    void testToString_unresolved() {
        assertThat(mayonaise).hasToString("Lazy.unresolved");
        assertThat(counter.get()).isZero();
    }

    @Test
    void testToString_unresolved_exception() {
        assertThat(exception).hasToString("Lazy.unresolved");
        assertThat(counter.get()).isZero();
    }

    @Test
    void testToString_resolved() {
        resolve(mayonaise);
        assertThat(mayonaise).hasToString("Lazy[I've seen them do it man, they f*n drown them in that shit!]");
        assertThat(counter.get()).isOne();
    }

    @Test
    void testToString_resolved_null() {
        Lazy<Object> lazyNull = Lazy.of(() -> null);
        assertThat(lazyNull.get()).isNull();
        assertThat(lazyNull).hasToString("Lazy[null]");

        Lazy<Object> eagerNull = Lazy.eager(null);
        assertThat(eagerNull.get()).isNull();
        assertThat(eagerNull).hasToString("Lazy[null]");
    }

    @Test
    void testToString_unresolved_due_to_exception() {
        resolve(exception);
        assertThat(exception).hasToString("Lazy.unresolved");
        assertThat(exception.isAvailable()).isFalse();
        assertThat(counter.get()).isOne();

        resolve(exception);
        assertThat(exception).hasToString("Lazy.unresolved");
        assertThat(exception.isAvailable()).isFalse();
        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    void testHashCode() {
        assertThat(Lazy.eager(null)).hasSameHashCodeAs(Lazy.of(() -> null));
        assertThat(Lazy.eager("Some value!")).hasSameHashCodeAs(Lazy.of(() -> "Some value!"));
        assertThat(Lazy.of(() -> "Some value!")).hasSameHashCodeAs("Some value!");
    }

    @Test
    void testEquals() {
        assertThat(Lazy.eager(null)).isEqualTo(Lazy.of(() -> null)).isNotEqualTo(null);
        assertThat(Lazy.of(() -> null)).isEqualTo(Lazy.eager(null))
                .isNotEqualTo(Lazy.of(() -> "")).isNotEqualTo(null);

        Lazy<String> lazy = Lazy.of(() -> "Some value!");
        assertThat(lazy)
                .isEqualTo(Lazy.of(lazy))
                .isEqualTo(Lazy.of(() -> "Some value!"))
                .isEqualTo(Lazy.eager("Some value!"))
                .isNotEqualTo(Lazy.of(() -> "Some other value!"))
                .isNotEqualTo("Some value!");
    }

    @Test
    @SuppressWarnings("deprecation")
    void testDeprecatedFactoryMethod() {
        Lazy<String> lazyString = Lazy.lazy(mayonaise);
        assertThat(lazyString.isAvailable()).isFalse();
        assertThat(mayonaise.isAvailable()).isFalse();

        assertThat(lazyString.get()).isEqualTo("I've seen them do it man, they f*n drown them in that shit!");
        assertThat(lazyString.get()).isEqualTo("I've seen them do it man, they f*n drown them in that shit!");

        assertThat(lazyString.isAvailable()).isTrue();
        assertThat(mayonaise.isAvailable()).isTrue();
        assertThat(counter.get()).isOne();
    }

    <T> Supplier<T> counting(Supplier<T> supplier) {
        return counting(counter, supplier);
    }

    static <T> Supplier<T> counting(AtomicInteger counter, Supplier<T> supplier) {
        return () -> {
            counter.incrementAndGet();
            return supplier.get();
        };
    }

    static <T, U> Function<T, U> counting(AtomicInteger counter, Function<T, U> function) {
        return input -> {
            counter.incrementAndGet();
            return function.apply(input);
        };
    }
}
