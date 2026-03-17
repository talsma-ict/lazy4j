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

/// Lazy evaluation for Java.
///
/// It is often useful to postpone expensive operations to the first time their value is actually needed.
/// Declaring a 'lazy supplier' function is a common pattern for this,
/// implemented by the [nl.talsmasoftware.lazy4j.Lazy] class.
///
/// ## Lazy values
/// The following class supports lazy values:
///
///   - [nl.talsmasoftware.lazy4j.Lazy] - a [java.util.function.Supplier] function for a lazily evaluated value.
///     After the first successful evaluation, the same result will be returned by all later calls.
///
/// ## Lazy collections
/// The following collections support lazy values:
///
///   - [nl.talsmasoftware.lazy4j.LazyList] - of lazy values,
///     behaving like a regular [java.util.List] with additional methods to interact with the lazy values.
///   - [nl.talsmasoftware.lazy4j.LazyValueMap] - of lazy values,
///     behaving like a regular [java.util.Map] with additional methods to interact with the lazy values.
///
/// @author Sjoerd Talsma
package nl.talsmasoftware.lazy4j;
