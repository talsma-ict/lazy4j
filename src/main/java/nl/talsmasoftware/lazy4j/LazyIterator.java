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

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Iterator for a lazy collection that allows access to the lazy, possibly unevaluated, value.
 *
 * @param <T> The type of elements being iterated.
 * @author Sjoerd Talsma
 * @since 2.0.3
 */
public interface LazyIterator<T> extends Iterator<T> {
    /**
     * Returns the next element in the iterator as a lazy value.
     *
     * <p>
     * This method advances the iterator the same way as {@link #next()}.
     *
     * @return The lazy next element.
     * @see #next()
     * @see #forEachRemainingAvailable(Consumer)
     */
    Lazy<T> nextLazy();

    /**
     * Performs the given action for each remaining element in the iterator, skipping not-yet-evaluated lazy values.
     *
     * <p>
     * This method will <strong>not</strong> eagerly evaluate any lazy values.
     * The iterator will be at the end of the collection (i.e) after this method returns.
     *
     * @param action The action to perform on each available remaining element.
     * @see #forEachRemaining(Consumer)
     */
    default void forEachRemainingAvailable(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        while (hasNext()) {
            Lazy<T> next = nextLazy();
            if (next != null) {
                next.ifAvailable(action);
            }
        }
    }
}
