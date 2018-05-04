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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A generic `Lazy` class in java.
 * <p>
 * The {@link #lazy(Supplier) lazy} factory method takes a {@linkplain Supplier} for a value
 * and wraps it so that it gets called <em>only when first-needed</em>.
 * <p>
 * A {@code Lazy} value can be {@linkplain #map(Function) mapped} or
 * {@linkplain #flatMap(Function) flat mapped} into another {@code Lazy} value.
 * <p>
 * {@code Lazy} objects are thread-safe, so no {@code Lazy} instance is evaluated more than once.
 *
 * @author Sjoerd Talsma
 */
public final class Lazy<T> implements Supplier<T> {

    private volatile Supplier<T> supplier;
    private volatile T result;
    private volatile RuntimeException exception;

    private Lazy(Supplier<T> supplier) {
        this.supplier = requireNonNull(supplier, "Lazy function is <null>.");
    }

    /**
     * Create a new {@linkplain Lazy} object that calls the specified {@code supplier}
     * only when the lazy value is first-needed.
     * <p>
     * For every {@link Lazy} instance, the supplier gets called <em>never or once</em>.
     * <p>
     * {@code Lazy} objects are thread-safe, so no {@code Lazy} instance is evaluated more than once.
     *
     * @param supplier The value supplier for the lazy placeholder
     * @param <T>      The type of the lazy value
     * @return A lazy placeholder for the supplier result that will only obtain it when needed.
     */
    public static <T> Lazy<T> lazy(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    /**
     * Returns the value from this lazy object, eagerly evaluating it if necessary.
     * <p>
     * This method is thread-safe, so no {@code Lazy} instance is evaluated more than once.
     *
     * @return The evaluated value from this lazy object
     */
    @Override
    public T get() {
        synchronized (this) {
            if (supplier != null) {
                try {
                    result = supplier.get();
                } catch (RuntimeException supplierException) {
                    exception = supplierException;
                }
                supplier = null;
            }
        }
        if (exception != null) {
            throw new LazyEvaluationException("Could not evaluate lazy value: " + exception.getMessage(), exception);
        }
        return result;
    }

    /**
     * Returns whether the lazy value was already evaluated and did not throw an exception.
     * <p>
     * This method can be used for conditional use of lazy values in cases where forced evaluation is not required.
     *
     * @return {@code true} if the lazy value was already evaluated, otherwise {@code false}.
     */
    public boolean isAvailable() {
        return supplier == null && exception == null;
    }

    /**
     * Provides the lazy value to the consumer <em>if it was already evaluated</em> and
     * will <b>not</b> eagerly evaluate the value to call the consumer.
     * <p>
     * The consumer will not be called if the lazy value was not already evaluated
     * or threw an exception.
     *
     * @param consumer The consumer to call if the lazy value is already available.
     */
    public void ifAvailable(Consumer<T> consumer) {
        requireNonNull(consumer, "Consumer of lazy value is <null>");
        if (isAvailable()) consumer.accept(get());
    }

    /**
     * Returns a new {@code Lazy} object with the result of applying the given mapping function
     * to the value contained in this lazy object.
     * <p>
     * Regardless whether {@code this} lazy object was already evaluated or not,
     * the mapping function will only be called when (and if) the returned {@code Lazy}
     * object's {@linkplain #get()} method is called.
     * <p>
     * Evaluating the returned {@code Lazy} object will also trigger eager evaluation
     * of {@code this} object.
     *
     * @param mapper the mapping function to lazily apply to this value
     * @param <U>    The type of the value returned from the mapping function
     * @return a new {@code Lazy} object with the result of applying a mapping
     * function to the value of this {@code Lazy} value
     * @throws NullPointerException if the mapping function is {@code null}
     */
    public <U> Lazy<U> map(Function<? super T, ? extends U> mapper) {
        requireNonNull(mapper, "Mapper function is <null>.");
        return lazy(() -> mapper.apply(get()));
    }

    public <U> Lazy<U> flatMap(Function<? super T, ? extends Supplier<? extends U>> mapper) {
        requireNonNull(mapper, "Mapper function is <null>.");
        return lazy(() -> requireNonNull(mapper.apply(get()), "Lazy supplier is <null>.").get());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                (supplier != null ? "[not yet resolved]"
                        : exception != null ? "[threw exception]"
                        : "[" + result + ']');
    }

}
