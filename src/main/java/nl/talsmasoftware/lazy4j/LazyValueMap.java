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
        this();
        putAll(toCopy);
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

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new LazyEntrySet<>(delegate.entrySet());
    }

    public Optional<V> getIfAvailable(K key) {
        return Optional.ofNullable(getIfAvailableElseNull(delegate.get(key)));
    }

    @Override
    public V get(Object key) {
        return getNullSafe(delegate.get(key));
    }

    @Override
    public V put(K key, V value) {
        return getIfAvailableElseNull(putLazy(key, Lazy.eager(value)));
    }

    public Lazy<V> putLazy(K key, Supplier<V> valueSupplier) {
        return delegate.put(key, Lazy.of(valueSupplier));
    }

    @Override
    public boolean containsValue(Object value) {
        // First pass, check available values.
        List<Lazy<V>> unavailableOnFirstPass = new ArrayList<>();
        for (Lazy<V> lazyValue : delegate.values()) {
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
        for (Lazy<V> lazyValue : unavailableOnFirstPass != null ? unavailableOnFirstPass : delegate.values()) {
            if (Objects.equals(value, lazyValue.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public V remove(Object key) {
        return getIfAvailableElseNull(delegate.remove(key));
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return getNullSafe(computeIfAbsentLazy(key, mappingFunction));
    }

    public Lazy<V> computeIfAbsentLazy(K key, Function<? super K, ? extends V> mappingFunction) {
        return delegate.computeIfAbsent(key, k -> Lazy.of(() -> mappingFunction.apply(k)));
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return getIfAvailableElseNull(putIfAbsentLazy(key, Lazy.eager(value)));
    }

    public Lazy<V> putIfAbsentLazy(K key, Supplier<V> value) {
        return delegate.putIfAbsent(key, Lazy.of(value));
    }

    @Override
    public V replace(K key, V value) {
        return getIfAvailableElseNull(replaceLazy(key, Lazy.eager(value)));
    }

    public Lazy<V> replaceLazy(K key, Supplier<V> value) {
        return delegate.replace(key, Lazy.of(value));
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        if (containsKey(key) && Objects.equals(oldValue, get(key))) {
            put(key, newValue);
            return true;
        }
        return false;
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return getNullSafe(computeIfPresentLazy(key, remappingFunction));
    }

    public Lazy<V> computeIfPresentLazy(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.computeIfPresent(key, (k, v) -> Lazy.of(() -> remappingFunction.apply(k, getNullSafe(v))));
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return getNullSafe(computeLazy(key, remappingFunction));
    }

    public Lazy<V> computeLazy(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.compute(key, (k, v) -> Lazy.of(() -> remappingFunction.apply(k, getNullSafe(v))));
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return getNullSafe(mergeLazy(key, Lazy.eager(value), remappingFunction));
    }

    public Lazy<V> mergeLazy(K key, Supplier<V> value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return delegate.merge(key, Lazy.of(value), (v1, v2) -> Lazy.of(() -> remappingFunction.apply(getNullSafe(v1), getNullSafe(v2))));
    }

    private static class LazyEntrySet<K, V> extends AbstractSet<Entry<K, V>> {
        private final Set<Entry<K, Lazy<V>>> delegateEntrySet;

        private LazyEntrySet(Set<Entry<K, Lazy<V>>> delegate) {
            this.delegateEntrySet = delegate;
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            final Iterator<Entry<K, Lazy<V>>> iterator = delegateEntrySet.iterator();
            return new Iterator<>() {
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

}
