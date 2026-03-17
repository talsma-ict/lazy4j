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

/// Internal utility class for null-safe interaction with [Lazy] objects.
///
/// @author Sjoerd Talsma
/// @since 2.0.3
final class LazyUtils {
    /// Private constructor for static utility class should never be called.
    private LazyUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /// Null-safe retrieval of the value from the lazy object.
    ///
    /// @param lazy The lazy object to retrieve the value from (optional, can be `null`).
    /// @param <V>  The type of the value to retrieve.
    /// @return The value from the lazy object, or `null` if the lazy object was `null`.
    /// @since 2.0.3
    static <V> V getNullSafe(Lazy<V> lazy) {
        return lazy == null ? null : lazy.get();
    }

    /// Null-safe check whether the lazy object is available.
    ///
    /// @param lazy The lazy object to check (optional, can be `null`).
    /// @return `true` if the lazy object is non-`null` and available, `false` otherwise.
    /// @since 2.0.3
    static boolean isAvailable(Lazy<?> lazy) {
        return lazy != null && lazy.isAvailable();
    }

    /// Null-safe, non-eager retrieval of the value from the lazy object.
    ///
    /// @param lazy The lazy object to retrieve the value from (optional, can be `null`).
    /// @param <T>  The type of the value to retrieve.
    /// @return The value from the lazy object, or `null` if the lazy object was `null` or not yet available.
    /// @since 2.0.3
    static <T> T getIfAvailableElseNull(Lazy<T> lazy) {
        return isAvailable(lazy) ? lazy.get() : null;
    }
}
