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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaxRetryTest {
    static final AtomicLong counter = new AtomicLong(0L);
    static final Supplier<String> throwingSupplier = () -> {
        counter.incrementAndGet();
        throw new IllegalStateException("Whoops!");
    };

    @BeforeEach
    void resetCounter() {
        counter.set(0L);
    }

    @Test
    void testMaxRetries_unlimited() {
        Lazy<String> lazy = Lazy.of(throwingSupplier);
        for (int i = 0; i < 100; i++) {
            assertThatThrownBy(lazy::get)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Whoops!");
        }
        assertThat(counter.get()).isEqualTo(100);
    }
}
