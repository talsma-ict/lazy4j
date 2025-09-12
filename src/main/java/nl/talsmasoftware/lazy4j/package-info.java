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
/**
 * Lazy evaluation in Java.
 *
 * <p>
 * This is often useful for expensive operations that need to be performed at most once,
 * and only if they are actually needed.<br>
 * Declaring a 'lazy supplier' function is a common pattern for this,
 * implemented by the {@link nl.talsmasoftware.lazy4j.Lazy} class.
 *
 * <h2>Lazy values</h2>
 * The following class supports lazy values:
 * <ul>
 *     <li>{@link nl.talsmasoftware.lazy4j.Lazy} - a {@link java.util.function.Supplier} function for a lazily evaluated value.
 *     After the first successful evaluation, the result is cached and reused for subsequent calls.
 * </ul>
 *
 * <h2>Lazy collections</h2>
 * The following collections support lazy values:
 * <ul>
 *     <li>{@link nl.talsmasoftware.lazy4j.LazyList} - of lazy values,
 *     behaving like a regular {@link java.util.List} with additional methods interacting with the lazy values.
 *     <li>{@link nl.talsmasoftware.lazy4j.LazyValueMap} - of lazy values,
 *     behaving like a regular {@link java.util.Map} with additional methods interacting with the lazy values.
 * </ul>
 *
 * @author Sjoerd Talsma
 */
package nl.talsmasoftware.lazy4j;
