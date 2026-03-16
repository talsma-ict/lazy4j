/*
 * Copyright 2018-2026 Talsma ICT
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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/// Generic `Lazy` class in java.
///
/// The [Lazy.of][#of(Supplier)] factory method takes a [Supplier]
/// and wraps it so that it gets called _only when first-needed_.
/// Results are _cached_.
///
/// A `Lazy` value can be [mapped][#map(Function)] or
/// [flat mapped][#flatMap(Function)] into another `Lazy` value.
/// The mapping function will only be called when the resulting value is actually needed for the first time.
///
/// `Lazy` objects are thread-safe.
/// For every [Lazy] instance,
/// the supplier gets called either _never_ or _once_*.
/// (*) There is one exception to this rule; if a RuntimeException is thrown,
/// the value is re-evaluated on successive invocations until the result is obtained without an exception.
///
/// @param <T> The type that is lazily evaluated.
/// @author Sjoerd Talsma
@SuppressWarnings("java:S3077") // We use reference-based thread safety, so volatile actually is what we want.
public final class Lazy<T> implements Supplier<T> {
    private volatile Supplier<T> supplier;
    private volatile T result;

    /// Create a [Lazy] object that calls the specified `supplier`
    /// only when the lazy value is needed for the first time.
    /// From the first successful(*) call, the result is re-used by the lazy instance.
    ///
    /// `Lazy` objects are thread-safe.
    ///
    /// (*) If the supplier throws an exception, the function call will be tried again on the next invocation,
    /// until no exception is thrown.
    ///
    /// @param supplier The value supplier for the lazy placeholder
    /// @param <T>      The type of the lazy value
    /// @return A lazy placeholder for the supplier result that will only obtain it when needed.
    /// @see #eager(Object)
    /// @since 2.0.1
    @SuppressWarnings("unchecked")
    public static <T> Lazy<T> of(Supplier<? extends T> supplier) {
        requireNonNull(supplier, "Lazy function is <null>.");
        return supplier instanceof Lazy ? (Lazy<T>) supplier : new Lazy<>(supplier::get, null);
    }

    /// Return an already-evaluated lazy object.
    ///
    /// This object is not really lazy, but it can be used in places where a lazy object is needed.
    /// [#isAvailable()] will always return `true` for the returned object.
    ///
    /// @param value The value to be returned as a lazy object.
    /// @param <T>   The type of the value.
    /// @return The already-evaluated lazy object always returning the same value.
    /// @see #of(Supplier)
    /// @see #isAvailable()
    /// @since 2.0.2
    public static <T> Lazy<T> eager(T value) {
        return new Lazy<>(null, value);
    }

    /// Constructor for a new lazy object.
    ///
    /// @param supplier The supplier.
    /// @param result   The result.
    private Lazy(Supplier<T> supplier, T result) {
        this.supplier = supplier;
        this.result = result;
    }

    /// Eagerly evaluates the lazy supplier (at most once)
    ///
    /// This uses double-checked locking which is safe in modern JVMs
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

    /// Returns the value from this lazy object, eagerly evaluating it if necessary.
    ///
    /// This method is thread-safe, so no `Lazy` instance is evaluated more than once.
    ///
    /// @return The evaluated value from this lazy object
    @Override
    public T get() {
        forceEagerEvaluation();
        return result;
    }

    /// Returns whether the lazy value was already evaluated and did not throw an exception.
    ///
    /// This method can be used for conditional use of lazy values in cases where forced evaluation is not required.
    ///
    /// @return `true` if the lazy value was already evaluated, otherwise `false`.
    /// @see #ifAvailable(Consumer)
    public boolean isAvailable() {
        return supplier == null;
    }

    /// Returns an optional reference to the value if it is already available,
    /// and will **not** eagerly evaluate the value otherwise,
    /// but just return [Optional#empty()] instead.
    ///
    /// The result of this method depends on the state of this lazy value
    /// but does not influence it.
    ///
    /// **Please note:** _This method cannot be used to determine whether or
    /// not the lazy value has already been evaluated.
    /// Please use [#isAvailable()] for that.
    /// A lazy object that evaluates to `null` can be evaluated
    /// and will still result in [Optional#empty()]._
    ///
    /// @return An optional wrapper for the value if it has already been evaluated
    /// and evaluates to non-`null`, or `Optional.empty()` otherwise.
    /// @see #isAvailable()
    /// @see #ifAvailable(Consumer)
    public Optional<T> getIfAvailable() {
        return isAvailable() ? Optional.ofNullable(get()) : Optional.empty();
    }

    /// Provides the lazy value to the consumer
    /// _if it is already [available][#isAvailable()]_
    /// and will **not** eagerly evaluate the value to call the consumer.
    ///
    /// The consumer will not be called if the lazy value was not already evaluated
    /// or threw an exception.
    ///
    /// **Please note:** _There is a difference between this method
    /// and calling `getIfAvailable().ifPresent(consumer)`. If the lazy value
    /// [is available][#isAvailable()] but evaluates to `null`,
    /// `ifAvailable(consumer)` will get called with evaluated value `null`,
    /// but `getIfAvailable().ifPresent(consumer)` will not get called with `null`_.
    ///
    /// @param consumer The consumer to call if the lazy value is already available.
    /// @see #isAvailable()
    /// @see #getIfAvailable()
    public void ifAvailable(Consumer<? super T> consumer) {
        requireNonNull(consumer, "Consumer of lazy value is <null>");
        if (isAvailable()) {
            consumer.accept(get());
        }
    }

    /// Returns a `Lazy` object with the result of applying the given mapping function
    /// to this lazy value.
    ///
    /// Regardless whether `this` lazy object was already evaluated or not,
    /// the mapping function will only be called when (and if) the returned `Lazy`
    /// object's [#get()] method is called.
    ///
    /// Evaluating the returned `Lazy` object will also trigger evaluation
    /// of `this` object, if necessary.
    ///
    /// @param <U>    The type of the value returned from the mapping function
    /// @param mapper the mapping function to lazily apply to this value
    /// @return a `Lazy` object with the result of applying a mapping
    /// function to the value in this `Lazy` instance
    /// @throws NullPointerException if the mapping function is `null`
    public <U> Lazy<U> map(Function<? super T, ? extends U> mapper) {
        requireNonNull(mapper, "Mapper function is <null>.");
        return Lazy.of(() -> mapper.apply(get()));
    }

    /// Returns a `Lazy` object with the result of applying the given mapping function
    /// to this lazy value.
    ///
    /// Regardless whether `this` lazy object was already evaluated or not,
    /// the mapping function will only be called when (and if) the returned `Lazy`
    /// object's [#get()] method is called.
    ///
    /// Both the mapping function and the resulting Supplier are
    /// lazily evaluated (and remembered) precisely when the resulting
    /// lazy value is evaluated.
    ///
    /// @param <U>    The type of the value returned by the resulting lazy reference
    /// @param mapper the mapping function to lazily apply to this value
    /// @return a `Lazy` object with the result of applying a mapping
    /// function to the value in this `Lazy` instance, calling the resulting
    /// `Supplier` only when the result is eagerly evaluated.
    /// @throws NullPointerException if the mapping function is `null`
    public <U> Lazy<U> flatMap(Function<? super T, ? extends Supplier<? extends U>> mapper) {
        requireNonNull(mapper, "Mapper function is <null>.");
        return Lazy.of(() -> requireNonNull(mapper.apply(get()), "Lazy mapper returned <null> supplier.").get());
    }

    /// Hashcode of this lazy object.
    ///
    /// **Note:** This eagerly evaluates the lazy value if necessary.
    ///
    /// @return The hashcode of this object.
    @Override
    public int hashCode() {
        return Objects.hashCode(get());
    }

    /// Checks equality to the other object.
    ///
    /// **Note:** If the other object is another Lazy object instance (i.e., not `this`),
    /// the values of both will be eagerly evaluated if necessary.
    ///
    /// @param other the object with which to compare.
    /// @return `true` if this object is equal the other object, `false` otherwise.
    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Lazy && Objects.equals(get(), ((Lazy<?>) other).get()));
    }

    /// String representation of this lazy object.
    ///
    ///   - `"Lazy.unresolved"` if the value was not yet resolved.
    ///   - `"Lazy[value]"` if the value was already resolved,
    ///     where `<value>` is equivalent to `Objects.toString(Lazy.get())`.
    ///
    ///
    /// @return String representation of this `Lazy` object.
    @Override
    public String toString() {
        return isAvailable() ? "Lazy[" + get() + ']' : "Lazy.unresolved";
    }

    /// Create a [Lazy] object that calls the specified `supplier`
    /// only when the lazy value is needed for the first time.
    /// From the first successful(*) call, the result is re-used by the lazy instance.
    ///
    /// `Lazy` objects are thread-safe.
    ///
    /// (*) If the supplier throws an exception, the function call will be tried again on the next invocation,
    /// until no exception is thrown.
    ///
    /// @param supplier The value supplier for the lazy placeholder
    /// @param <T>      The type of the lazy value
    /// @return A lazy placeholder for the supplier result that will only obtain it when needed.
    /// @see #of(Supplier)
    /// @deprecated Factory method was renamed to `Lazy.of` in version 2.0.1
    @SuppressWarnings("java:S6355")
    @Deprecated // Java 8 does not yet support Deprecated annotation arguments (forRemoval = true, since = "2.0.1")
    public static <T> Lazy<T> lazy(Supplier<T> supplier) {
        return Lazy.of(supplier);
    }
}
