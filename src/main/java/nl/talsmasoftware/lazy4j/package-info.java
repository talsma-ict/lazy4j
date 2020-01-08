/*
 * Copyright 2018-2020 Talsma ICT
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
 * A generic {@link Lazy} class for java.
 * <p>
 * Java 8 defines a generic {@code Optional} type and a {@code Supplier} function for use with lambda's,
 * but unfortunately, there is no such thing as a 'lazy supplier function that re-uses the result'.
 * <p>
 * This is often useful for expensive operations that need to be performed at most once,
 * only if they are actually needed.
 * Declaring a 'lazy supplier' function is a common pattern for this
 * and can be easily defined with the introduction of Lambda's in Java 8.
 *
 * @author Sjoerd Talsma
 */
package nl.talsmasoftware.lazy4j;