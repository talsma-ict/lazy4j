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
 * A generic {@link nl.talsmasoftware.lazy4j.Lazy} class for java.
 *
 * <p>
 * Java 8 defines a generic {@link java.util.Optional} type and a {@link java.util.function.Supplier} function
 * for use with lambda's, but unfortunately, there is no such thing as
 * a 'lazy supplier function that re-uses the result'.
 *
 * <p>
 * This is often useful for expensive operations that need to be performed at most once,
 * only if they are actually needed.
 * Declaring a 'lazy supplier' function is a common pattern for this
 * and can be easily defined with the introduction of Lambda's in Java 8.
 *
 * <h2>Lazy collections</h2>
 * The following collection implementations are provided containing lazy values:
 * <ul>
 *     <li>{@link nl.talsmasoftware.lazy4j.LazyList} - a {@link java.util.List} of lazy values,
 *     behaving like a regular list.
 *     <li>{@link nl.talsmasoftware.lazy4j.LazyValueMap} - a {@link java.util.Map} of lazy values,
 *     behaving like a regular map.
 * </ul>
 *
 * @author Sjoerd Talsma
 */
package nl.talsmasoftware.lazy4j;