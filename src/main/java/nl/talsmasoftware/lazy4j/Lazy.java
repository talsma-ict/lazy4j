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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A generic `Lazy` class in java.
 * <p>
 * The {@link #lazy(Supplier) lazy} factory method takes a {@linkplain Supplier} for a value
 * and wraps it so that it gets called <em>only when first-needed</em>.<br>
 * Results are <em>cached</em>.
 * <p>
 * A {@code Lazy} value can be {@linkplain #map(Function) mapped} or
 * {@linkplain #flatMap(Function) flat mapped} into another {@code Lazy} value.
 * <p>
 * {@code Lazy} objects are thread-safe. For every {@link Lazy} instance,
 * the supplier gets called either <em>never</em> or <em>once</em>.
 * <p>
 * There is one exception to this rule; if a RuntimeException is thrown
 * the value is re-evaluated until the result is obtained without an exception.
 *
 * @author Sjoerd Talsma
 */
public final class Lazy<T> implements Supplier<T> {

    private volatile Supplier<T> supplier;
    private volatile T result;

    /**
     * Constructor for unevaluated lazy object.
     *
     * @param supplier The supplier (required)
     */
    private Lazy(Supplier<T> supplier) {
        this.supplier = requireNonNull(supplier, "Lazy function is <null>.");
    }

    /**
     * Create a {@linkplain Lazy} object that calls the specified {@code supplier}
     * only when the lazy value is needed for the first time.
     * From the first successful(*) call, the result is re-used by the lazy instance.
     * <p>
     * {@code Lazy} objects are thread-safe.
     * <p>
     * (*) If the supplier throws an exception, the function call will be tried again
     * on the next invocation, until no exception is thrown.
     *
     * @param supplier The value supplier for the lazy placeholder
     * @param <T>      The type of the lazy value
     * @return A lazy placeholder for the supplier result that will only obtain it when needed.
     */
    public static <T> Lazy<T> lazy(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    /**
     * Eagerly evaluates the lazy supplier (at most once)
     * <p>
     * Please note: this uses double-checked locking which is safe in modern JVMs
     */
    private void forceEagerEvaluation() {
        if (supplier != null) {
            synchronized (this) {
                if (supplier != null) {
                    result = supplier.get();
                    supplier = null;
                }
            }
        }
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
        forceEagerEvaluation();
        return result;
    }

    /**
     * Returns whether the lazy value was already evaluated and did not throw an exception.
     * <p>
     * This method can be used for conditional use of lazy values in cases where forced evaluation is not required.
     *
     * @return {@code true} if the lazy value was already evaluated, otherwise {@code false}.
     * @see #ifAvailable(Consumer)
     */
    public boolean isAvailable() {
        return supplier == null;
    }

    /**
     * Returns an optional reference to the value if it is already available,
     * and will <b>not</b> eagerly evaluate the value otherwise,
     * but just return {@linkplain Optional#empty()} instead.
     * <p>
     * The result of this method depends on the state of this lazy value
     * but does not influence it.
     * <p>
     * <b>Please note:</b> <em>This method cannot be used to determine whether or
     * not the lazy value has already been evaluated.
     * Please use {@linkplain #isAvailable()} for that.
     * A lazy object that evaluates to {@code null} can be evaluated
     * and will still result in {@linkplain Optional#empty()}.</em>
     *
     * @return An optional wrapper for the value if it has already been evaluated
     * and evaluates to non-{@code null}, or {@code Optional.empty()} otherwise.
     * @see #isAvailable()
     * @see #ifAvailable(Consumer)
     */
    public Optional<T> getIfAvailable() {
        return isAvailable() ? Optional.ofNullable(get()) : Optional.empty();
    }

    /**
     * Provides the lazy value to the consumer
     * <em>if it is already {@linkplain #isAvailable() available}</em>
     * and will <b>not</b> eagerly evaluate the value to call the consumer.
     * <p>
     * The consumer will not be called if the lazy value was not already evaluated
     * or threw an exception.
     * <p>
     * <b>Please note:</b> <em>There is a difference between this method
     * and calling {@code getIfAvailable().ifPresent(consumer)}. If the lazy value
     * {@linkplain #isAvailable() is available} but evaluates to {@code null},
     * {@code ifAvailable(consumer)} will get called with evaluated value {@code null},
     * but {@code getIfAvailable().ifPresent(consumer)} will not get called with {@code null}</em>
     *
     * @param consumer The consumer to call if the lazy value is already available.
     * @see #isAvailable()
     * @see #getIfAvailable()
     */
    public void ifAvailable(Consumer<T> consumer) {
        requireNonNull(consumer, "Consumer of lazy value is <null>");
        if (isAvailable()) consumer.accept(get());
    }

    /**
     * Returns a {@code Lazy} object with the result of applying the given mapping function
     * to this lazy value.
     * <p>
     * Regardless whether {@code this} lazy object was already evaluated or not,
     * the mapping function will only be called when (and if) the returned {@code Lazy}
     * object's {@linkplain #get()} method is called.
     * <p>
     * Evaluating the returned {@code Lazy} object will also trigger eager evaluation
     * of {@code this} object.
     *
     * @param <U>    The type of the value returned from the mapping function
     * @param mapper the mapping function to lazily apply to this value
     * @return a {@code Lazy} object with the result of applying a mapping
     * function to the value in this {@code Lazy} instance
     * @throws NullPointerException if the mapping function is {@code null}
     */
    public <U> Lazy<U> map(Function<? super T, ? extends U> mapper) {
        requireNonNull(mapper, "Mapper function is <null>.");
        return lazy(() -> mapper.apply(get()));
    }

    /**
     * Returns a {@code Lazy} object with the result of applying the given mapping function
     * to this lazy value.
     * <p>
     * Regardless whether {@code this} lazy object was already evaluated or not,
     * the mapping function will only be called when (and if) the returned {@code Lazy}
     * object's {@linkplain #get()} method is called.
     * <p>
     * The resulting {@code Supplier} from the mapping function is
     * eagerly evaluated (a most once) precisely when the resulting
     * lazy value is evaluated.
     *
     * @param <U>    The type of the value returned by the resulting lazy reference
     * @param mapper the mapping function to lazily apply to this value
     * @return a {@code Lazy} object with the result of applying a mapping
     * function to the value in this {@code Lazy} instance, calling the resulting
     * {@code Supplier} only when the result is eagerly evaluated.
     * @throws NullPointerException if the mapping function is {@code null}
     */
    public <U> Lazy<U> flatMap(Function<? super T, ? extends Supplier<? extends U>> mapper) {
        requireNonNull(mapper, "Mapper function is <null>.");
        return lazy(() -> requireNonNull(mapper.apply(get()), "Lazy supplier is <null>.").get());
    }

    /**
     * String representation of this lazy object.
     * <ul>
     * <li>{@code "Lazy[not yet resolved]"} if the value was not yet resolved.
     * <li>{@code "Lazy[<value>]"} if the value was already resolved,
     * where {@code <value>} is equivalent to {@code Objects.toString(Lazy.get())}.
     * </ul>
     *
     * @return String representation of this {@code Lazy} object.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() +
                (supplier != null ? "[not yet resolved]" : "[" + result + ']');
    }

}
