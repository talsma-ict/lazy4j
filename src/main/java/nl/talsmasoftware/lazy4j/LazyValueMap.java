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

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static nl.talsmasoftware.lazy4j.LazyUtils.getIfAvailableElseNull;
import static nl.talsmasoftware.lazy4j.LazyUtils.getNullSafe;

/**
 * A map that can store its values in a {@link Lazy} manner.
 *
 * <p>
 * This has the advantage that while providing 'standard' {@link Map} features,
 * unused values do not need to be evaluated.
 *
 * <p>
 * The behaviour of this map depends on the delegate map it was {@link #LazyValueMap(Supplier) initialized} with.
 * If the delegate map is mutable, the lazy map will be mutable as well.
 * If the delegate map is sorted, the lazy map will be sorted as well.
 * The {@link #LazyValueMap() default} and {@link #LazyValueMap(Map) copy} constructors create a lazy map
 * that behaves like a {@link LinkedHashMap}.
 *
 * @param <K> The type of the keys in the map.
 * @param <V> The type of the values in the map.
 * @author Sjoerd Talsma
 * @see Lazy
 * @since 2.0.2
 */
public class LazyValueMap<K, V> extends AbstractMap<K, V> {
    /**
     * The delegate map containing the lazy values.
     */
    private final Map<K, Lazy<V>> delegate;

    /**
     * Creates a new empty {@code LazyValueMap}, behaving like a {@link LinkedHashMap}.
     *
     * @see #LazyValueMap(Supplier)
     */
    public LazyValueMap() {
        this(LinkedHashMap::new);
    }

    /**
     * Creates a new {@code LazyValueMap} with the same contents as the specified map.
     *
     * <p>
     * The new map behaves like a {@link LinkedHashMap}.
     *
     * @param toCopy The map to copy the contents from.
     * @see #LazyValueMap(Supplier)
     */
    public LazyValueMap(Map<K, V> toCopy) {
        this(() -> copyToLazyMap(toCopy));
    }

    /**
     * Creates a new {@code LazyValueMap} backed by a map created by the specified map factory.
     *
     * <p>
     * This offers full control over the backing map implementation.
     * For instance:
     * <pre>{@code
     * Map<K, V> lazyHashMap = new LazyValueMap<>(HashMap::new);
     * Map<K, V> lazyLinkedHashMap = new LazyValueMap<>(LinkedHashMap::new);
     * Map<K, V> lazyTreeMap = new LazyValueMap<>(TreeMap::new);
     * }</pre>
     *
     * <p>
     * Technically, the mapFactory does not <em>need</em> to create a new map,
     * but please be aware that sharing the delegate map may cause unexpected behaviour,
     * especially if it is accessed concurrently.
     *
     * @param mapFactory The map factory to provide the backing map (required, must provide a non-{@code null} map).
     */
    public LazyValueMap(Supplier<Map<K, Lazy<V>>> mapFactory) {
        this.delegate = requireNonNull(mapFactory.get(), "Backing map may not be <null>.");
    }

    /**
     * Returns a {@code Set} view of the mappings contained in this map.
     *
     * <p>
     * The set supports mutation operations such as removal and {@link Map.Entry#setValue(Object)} calls
     * <em>if</em> the backing map supports them.
     *
     * @return A {@code Set} view of the mapping entries contained in this map.
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        return new LazyEntrySet<>(delegate.entrySet());
    }

    /**
     * Gets the value for the specified key, if it is already available.
     *
     * <p>
     * Will return the evaluated value if:
     * <ol>
     *     <li>The map contains a lazy value for the specified key that has already been evaluated and is not {@code null}.
     * </ol>
     *
     * <p>
     * Will return {@link Optional#empty()} if:
     * <ol>
     *     <li>The map does not contain the specified key.
     *     <li>The map contains a lazy value for the specified key that has not been evaluated yet.
     *     <li>The map contains an evaluated value that is {@code null}.
     * </ol>
     *
     * <p>
     * Note that this method will <em>not</em> force the mapped value to be evaluated.
     *
     * @param key The key to get the value for, if already available.
     * @return The value for the specified key, if already available, otherwise {@link Optional#empty()}.
     * @see Lazy#getIfAvailable()
     */
    public Optional<V> getIfAvailable(K key) {
        return Optional.ofNullable(getIfAvailableElseNull(getLazy(key)));
    }

    /**
     * Gets the value for the specified key, forcing the lazy value to be evaluated if necessary.
     *
     * @param key the key whose associated value is to be returned
     * @return The value for the specified key, or {@code null} if the map contains no mapping for the key.
     * @see #getIfAvailable(Object)
     * @see #getLazy(Object)
     */
    @Override
    public V get(Object key) {
        return getNullSafe(getLazy(key));
    }

    /**
     * Gets the lazy value for the specified key.
     *
     * @param key the key whose associated value is to be returned
     * @return The lazy value for the specified key, or {@code null} if the map contains no mapping for the key.
     */
    public Lazy<V> getLazy(Object key) {
        return delegate.get(key);
    }

    /**
     * Puts the specified value for the specified key.
     *
     * <p>
     * This value will <strong>not</strong> be lazy and will be available immediately.
     *
     * @param key   the key with which the specified value is to be associated.
     * @param value the new value to be associated with the specified key.
     * @return The previous value associated with key,  or {@code null} if there was no mapping for key.
     * A null result can also indicate that <strong>the previous lazy value was not yet available</strong>.
     * @implNote The returned previous mapping, if any, is <em>not</em> eagerly evaluated by this method.
     * @see #putLazy(Object, Supplier)
     */
    @Override
    public V put(K key, V value) {
        return getIfAvailableElseNull(putLazy(key, Lazy.eager(value)));
    }

    /**
     * Associates the specified key with a lazily computed value.
     *
     * <p>
     * The supplied {@link Supplier} will be evaluated at most once,
     * the first time the value is required (e.g. via {@link #get(Object)} or by an operation that
     * needs the concrete value).
     *
     * @param key           the key with which the specified lazy value is to be associated.
     * @param valueSupplier supplier that provides the value when needed (required, non-{@code null}).
     * @return the previous value associated with the key, or {@code null} if there was no mapping.
     * @implNote The returned previous mapping, if any, is <em>not</em> eagerly evaluated by this method.
     * @see #put(Object, Object)
     * @see Lazy#of(Supplier)
     */
    public Lazy<V> putLazy(K key, Supplier<V> valueSupplier) {
        return delegate.put(key, Lazy.of(valueSupplier));
    }

    /**
     * Returns a {@link Collection} view of the lazy values contained in this map.
     *
     * @return The lazy values contained in this map.
     * @see #values()
     */
    public Collection<Lazy<V>> lazyValues() {
        return delegate.values();
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the specified value.
     *
     * <p>
     * This implementation performs a two-pass check to minimize unnecessary evaluations:
     * it first inspects already available values, and only evaluates unavailable values on a second pass
     * if no match was found. This means calling this method may cause evaluation of previously
     * unavailable lazy values.
     *
     * @param value value whose presence in this map is to be tested.
     * @return {@code true} if this map maps one or more keys to the specified value.
     */
    @Override
    public boolean containsValue(Object value) {
        // First pass, check available values.
        List<Lazy<V>> unavailableOnFirstPass = new ArrayList<>();
        for (Lazy<V> lazyValue : lazyValues()) {
            if (!lazyValue.isAvailable()) {
                if (unavailableOnFirstPass != null) {
                    if (unavailableOnFirstPass.size() < 10000) {
                        unavailableOnFirstPass.add(lazyValue);
                    } else { // prevent excessive memory usage.
                        unavailableOnFirstPass = null;
                    }
                }
            } else if (Objects.equals(value, lazyValue.get())) {
                return true;
            }
        }
        // Second pass, check the values that were not yet available.
        for (Lazy<V> lazyValue : unavailableOnFirstPass != null ? unavailableOnFirstPass : lazyValues()) {
            if (Objects.equals(value, lazyValue.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map.
     */
    @Override
    public int size() {
        return delegate.size();
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     *
     * <p>
     * This operation does not evaluate the associated value, if any.
     *
     * @param key key whose presence in this map is to be tested.
     * @return {@code true} if this map contains a mapping for the specified key.
     */
    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * <p>
     * The removed value is returned only if it had already been evaluated; otherwise {@code null} is returned
     * without forcing evaluation.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return the previously associated value if it was available, or {@code null} otherwise.
     */
    @Override
    public V remove(Object key) {
        return getIfAvailableElseNull(delegate.remove(key));
    }

    /**
     * Removes all the mappings from this map.
     *
     * <p>
     * After this call returns, the map will be empty.
     */
    @Override
    public void clear() {
        delegate.clear();
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     *
     * <p>
     * The returned set is backed by the map; changes to the set are reflected in the map, and vice-versa.
     *
     * @return a set view of the keys contained in this map.
     */
    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    /**
     * If the specified key is not already associated with a value, attempts to compute its value using
     * the given mapping function and enters it into this map.
     *
     * <p>
     * This forces the computation of an existing lazy value, if necessary.
     *
     * @param key             key with which the computed value is to be associated.
     * @param mappingFunction the function to compute a value.
     * @return the current (existing or computed) value associated with the specified key, or {@code null} if none.
     * @see #computeIfAbsentLazy(Object, Function)
     */
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return getNullSafe(computeIfAbsentLazy(key, mappingFunction));
    }

    /**
     * Lazy variant of {@link #computeIfAbsent(Object, Function)}.
     *
     * <p>
     * Stores a lazy computation for the value if the key is not present.
     * The mapping function itself is not executed until the value is required for the first time.
     *
     * @param key             key with which the computed value is to be associated.
     * @param mappingFunction the function to compute a value.
     * @return the lazy value associated with the specified key (existing or newly created).
     */
    public Lazy<V> computeIfAbsentLazy(K key, Function<? super K, ? extends V> mappingFunction) {
        return delegate.computeIfAbsent(key, k -> Lazy.of(() -> mappingFunction.apply(k)));
    }

    /**
     * If the specified key is not already associated with a value, associate it with the given value.
     *
     * <p>
     * The provided value is stored eagerly. If a mapping already exists, that existing value is left
     * untouched and returned if it was already available; otherwise {@code null} is returned without
     * forcing evaluation of the existing lazy value.
     *
     * @param key   key with which the specified value is to be associated.
     * @param value value to associate with the specified key if absent.
     * @return the previous value if present and already available; otherwise {@code null}.
     * @see #putIfAbsentLazy(Object, Supplier)
     */
    @Override
    public V putIfAbsent(K key, V value) {
        return getIfAvailableElseNull(putIfAbsentLazy(key, Lazy.eager(value)));
    }

    /**
     * Lazy variant of {@link #putIfAbsent(Object, Object)}.
     *
     * <p>
     * If the key is absent, associates it with a lazy value constructed from the given supplier.
     * If a mapping already exists, it is returned and not evaluated by this method.
     *
     * @param key   key with which the specified lazy value is to be associated.
     * @param value supplier of the value to associate if absent (required, non-{@code null}).
     * @return the existing lazy value if present, or {@code null} if the association was added.
     */
    public Lazy<V> putIfAbsentLazy(K key, Supplier<V> value) {
        return delegate.putIfAbsent(key, Lazy.of(value));
    }

    /**
     * Replaces the entry for the specified key only if it is currently mapped to some value.
     *
     * <p>
     * The new value is stored eagerly. The previously associated value is returned only if it had
     * already been evaluated; otherwise {@code null} is returned without forcing evaluation.
     *
     * @param key   key with which the specified value is associated.
     * @param value value to be associated with the specified key.
     * @return the previous value if present and available; otherwise {@code null}.
     * @see #replaceLazy(Object, Supplier)
     */
    @Override
    public V replace(K key, V value) {
        return getIfAvailableElseNull(replaceLazy(key, Lazy.eager(value)));
    }

    /**
     * Lazy variant of {@link #replace(Object, Object)}.
     *
     * <p>
     * Replaces the entry for the specified key only if it is currently mapped to some value,
     * associating it with the given lazy supplier. The previous lazy value, if any, is returned
     * without being evaluated by this method.
     *
     * @param key   key with which the specified lazy value is associated.
     * @param value supplier of the new value (required, non-{@code null}).
     * @return the previous lazy value associated with the key, or {@code null} if there was no mapping.
     */
    public Lazy<V> replaceLazy(K key, Supplier<V> value) {
        return delegate.replace(key, Lazy.of(value));
    }

    /**
     * If the value for the specified key is present, computes a new mapping given the key and its current value.
     *
     * <p>
     * If the map contains a lazy value for the specified key,
     * it will be eagerly evaluated and applied in the remapping function.
     * Consider using {@link #computeIfPresentLazy(Object, BiFunction)} instead, to prevent eager evaluation.
     *
     * @param key               key with which the specified value is associated.
     * @param remappingFunction the function to compute a value.
     * @return the new value associated with the specified key, or {@code null} if none.
     * @see #computeIfPresentLazy(Object, BiFunction)
     */
    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return getNullSafe(computeIfPresentLazy(key, remappingFunction));
    }

    /**
     * Lazy variant of {@link #computeIfPresent(Object, BiFunction)}.
     *
     * <p>
     * If a non-null mapping exists, stores a lazy remapping that invokes {@code remappingFunction}
     * with the key and the evaluated current value when needed.
     *
     * @param key               key with which the specified value is associated.
     * @param remappingFunction the function to compute a value.
     * @return the new lazy value associated with the specified key, or {@code null} if none.
     */
    public Lazy<V> computeIfPresentLazy(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.computeIfPresent(key, (k, v) -> Lazy.of(() -> remappingFunction.apply(k, getNullSafe(v))));
    }

    /**
     * Compute a mapping for the specified key and its current mapped value (or {@code null}
     * if there is no current mapping).
     *
     * <p>
     * If the map contains a lazy value for the specified key,
     * it will be eagerly evaluated and applied in the remapping function.
     * Consider using {@link #computeLazy(Object, BiFunction)} instead, to prevent eager evaluation.
     *
     * @param key               key with which the specified value is associated.
     * @param remappingFunction the function to compute a value.
     * @return the new value associated with the specified key, or {@code null} if none.
     * @see #computeLazy(Object, BiFunction)
     */
    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return getNullSafe(computeLazy(key, remappingFunction));
    }

    /**
     * Lazy variant of {@link #compute(Object, BiFunction)}.
     *
     * <p>
     * Stores a lazy remapping that invokes {@code remappingFunction} with the key and the evaluated
     * current value (which may be {@code null}), when needed.
     *
     * <p>
     * The remapping function (and eager evaluation of the existing value) is applied lazily and will only be evaluated
     * when the computed value is actually needed for the first time.
     *
     * @param key               key with which the specified value is associated.
     * @param remappingFunction the function to compute a value.
     * @return the new lazy value associated with the specified key.
     */
    public Lazy<V> computeLazy(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.compute(key, (k, v) -> Lazy.of(() -> remappingFunction.apply(k, getNullSafe(v))));
    }

    /**
     * If the specified key is not already associated with a value, associates it with the given value.
     * Otherwise, replaces the value with the result of the given remapping function.
     *
     * <p>
     * The provided value is stored eagerly; the remapping function is applied lazily and will be evaluated
     * when the merged value is actually needed.
     *
     * @param key               key with which the resulting value is to be associated.
     * @param value             the value to use if absent.
     * @param remappingFunction the function to recompute a value if present.
     * @return the new value associated with the specified key, or {@code null} if none.
     * @see #mergeLazy(Object, Supplier, BiFunction)
     */
    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return getNullSafe(mergeLazy(key, Lazy.eager(value), remappingFunction));
    }

    /**
     * Lazy variant of {@link #merge(Object, Object, BiFunction)}.
     *
     * <p>
     * If the key is absent, associates it with a lazy value from the given supplier. If present,
     * associates it with a lazy value that lazily applies the {@code remappingFunction} to the
     * existing and supplied values only when needed for the first time.
     *
     * @param key               key with which the resulting value is to be associated.
     * @param value             supplier of the value to use if absent (required, non-{@code null}).
     * @param remappingFunction the function to recompute a value if present.
     * @return the new lazy value associated with the specified key.
     * @throws NullPointerException if {@code value} is {@code null} (via {@link Lazy#of(Supplier)}).
     */
    public Lazy<V> mergeLazy(K key, Supplier<V> value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return delegate.merge(key, Lazy.of(value), (v1, v2) -> Lazy.of(() -> remappingFunction.apply(getNullSafe(v1), getNullSafe(v2))));
    }

    /**
     * EntrySet that exposes {@code Map.Entry<K,V>} views while delegating to an underlying
     * {@code Set<Entry<K, Lazy<V>>>}. Values are evaluated on access only where required.
     *
     * @param <K> key type.
     * @param <V> value type.
     */
    private static class LazyEntrySet<K, V> extends AbstractSet<Entry<K, V>> {
        private final Set<Entry<K, Lazy<V>>> delegateEntrySet;

        private LazyEntrySet(Set<Entry<K, Lazy<V>>> delegate) {
            this.delegateEntrySet = delegate;
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            final Iterator<Entry<K, Lazy<V>>> iterator = delegateEntrySet.iterator();
            return new Iterator<Entry<K, V>>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Entry<K, V> next() {
                    return new LazyEntry<>(iterator.next());
                }

                @Override
                public void remove() {
                    iterator.remove();
                }
            };
        }

        @Override
        public int size() {
            return delegateEntrySet.size();
        }
    }

    /**
     * Map.Entry wrapper that exposes an evaluated {@code V} view over an underlying
     * {@code Entry<K, Lazy<V>>}. Setting a value replaces the underlying lazy value with
     * an eager one for the provided value.
     *
     * @param <K> key type.
     * @param <V> value type.
     */
    private static final class LazyEntry<K, V> implements Entry<K, V> {
        private final Entry<K, Lazy<V>> delegateEntry;

        private LazyEntry(Entry<K, Lazy<V>> delegate) {
            this.delegateEntry = delegate;
        }

        @Override
        public K getKey() {
            return delegateEntry.getKey();
        }

        @Override
        public V getValue() {
            return getNullSafe(delegateEntry.getValue());
        }

        @Override
        public V setValue(V value) {
            return getIfAvailableElseNull(delegateEntry.setValue(Lazy.eager(value)));
        }
    }

    /**
     * Creates a {@code Map<K, Lazy<V>>} copy of the given {@code Map<K, V>}.
     *
     * <p>
     * If the source map is already a {@code LazyValueMap}, its internal lazy values are reused
     * without triggering evaluation. Otherwise, values are wrapped into eager {@link Lazy} instances.
     *
     * @param toCopy source map to copy from.
     * @param <K>    key type.
     * @param <V>    value type.
     * @return a new {@link LinkedHashMap} containing lazy values that mirror the source contents.
     */
    private static <K, V> Map<K, Lazy<V>> copyToLazyMap(Map<K, V> toCopy) {
        if (toCopy instanceof LazyValueMap) { // Reuse lazy values if we can.
            return new LinkedHashMap<>(((LazyValueMap<K, V>) toCopy).delegate);
        }

        // Otherwise, convert the actual values to (eager) Lazy instances.
        Map<K, Lazy<V>> eagerCopy = new LinkedHashMap<>(toCopy.size());
        for (Entry<K, V> entry : toCopy.entrySet()) {
            eagerCopy.put(entry.getKey(), Lazy.eager(entry.getValue()));
        }
        return eagerCopy;
    }
}
