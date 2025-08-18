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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static nl.talsmasoftware.lazy4j.LazyUtils.getIfAvailableElseNull;
import static nl.talsmasoftware.lazy4j.LazyUtils.getNullSafe;

public class LazyList<T> extends AbstractList<T> {
    /**
     * The delegate list containing the lazy values.
     */
    private final List<Lazy<T>> delegate;

    /**
     * Creates a new empty {@code LazyList}, behaving like an {@link ArrayList}.
     *
     * @see #LazyList(Supplier)
     */
    public LazyList() {
        this(ArrayList::new);
    }

    /**
     * Creates a new {@code LazyList} with the same contents as the specified collection.
     *
     * <p>
     * The new list behaves like a {@link ArrayList}.
     *
     * @param toCopy The collection to copy the contents from.
     * @see #LazyList(Supplier)
     */
    public LazyList(Collection<? extends T> toCopy) {
        this(() -> copyToLazyList(toCopy));
    }

    /**
     * Creates a new {@code LazyList} backed by a list created by the specified map factory.
     *
     * <p>
     * This offers full control over the backing list implementation.
     * For instance:
     * <pre>{@code
     * List<T> lazyArrayList = new LazyValueList<>(ArrayList::new);
     * List<T> lazyLinkedList = new LazyValueList<>(LinkedList::new);
     * }</pre>
     *
     * <p>
     * Technically, the listFactory does not <em>need</em> to create a new list,
     * but please be aware that sharing the delegate list may cause unexpected behaviour,
     * especially if it is accessed concurrently.
     *
     * @param listFactory The list factory to provide the backing list (required, must provide a non-{@code null} list).
     */
    public LazyList(Supplier<List<Lazy<T>>> listFactory) {
        this.delegate = requireNonNull(listFactory.get(), "Backing list may not be <null>.");
    }

    /**
     * Returns the element at the specified position in this list as a lazy value.
     *
     * @param index The index of the element to return.
     * @return The lazy value at the specified position.
     * @see #get(int)
     * @see #getFirstLazy()
     * @see #getLastLazy()
     */
    public Lazy<T> getLazy(int index) {
        return delegate.get(index);
    }

    /**
     * Returns the first element of this list as a lazy value.
     *
     * @return The lazy first element.
     * @see #get(int)
     * @see #getLazy(int)
     * @see #getLastLazy()
     */
    public Lazy<T> getFirstLazy() {
        return getLazy(0);
    }

    /**
     * Returns the last element of this list as a lazy value.
     *
     * @return The lazy last element.
     * @see #get(int)
     * @see #getLazy(int)
     * @see #getFirstLazy()
     */
    public Lazy<T> getLastLazy() {
        return getLazy(size() - 1);
    }

    /**
     * Sets the element at the specified position in this list to the specified element.
     *
     * <p>
     * The actual value of the element is only evaluated when it is needed for the first time.
     *
     * @param index   The index of the element to replace.
     * @param element The supplier for the new element. The supplier will be evaluated only if necessary.
     * @return The previous element at the specified position.
     */
    public Lazy<T> setLazy(int index, Supplier<? extends T> element) {
        return delegate.set(index, Lazy.of(element));
    }

    public boolean addLazy(Supplier<? extends T> lazy) {
        return delegate.add(Lazy.of(lazy));
    }

    public void addLazy(int index, Supplier<? extends T> lazy) {
        delegate.add(index, Lazy.of(lazy));
    }

    public void addFirstLazy(Supplier<? extends T> lazy) {
        addLazy(0, lazy);
    }

    public void addLastLazy(Supplier<? extends T> lazy) {
        addLazy(lazy);
    }

    public boolean addAllLazy(int index, Collection<Lazy<T>> lazyValues) {
        return delegate.addAll(index, lazyValues);
    }

    public Lazy<T> removeLazy(int index) {
        return delegate.remove(index);
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * <p>
     * The corresponding lazy value is evaluated if necessary.
     *
     * @param index index of the element to return.
     * @return The element at the specified position.
     * @see #getLazy(int)
     */
    @Override
    public T get(int index) {
        return getNullSafe(getLazy(index));
    }

    @Override
    public T set(int index, T element) {
        return getIfAvailableElseNull(setLazy(index, Lazy.eager(element)));
    }

    @Override
    public boolean add(T value) {
        return addLazy(Lazy.eager(value));
    }

    @Override
    public void add(int index, T element) {
        addLazy(index, Lazy.eager(element));
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return addAllLazy(index, c.stream()
                .map(v -> Lazy.eager((T) v))
                .collect(toCollection(ArrayList::new)));
    }

    /**
     * Removes the element at the specified position in this list.
     *
     * <p>
     * The removed value is returned only if it had already been evaluated; otherwise {@code null} is returned
     * without forcing evaluation.
     *
     * @param index the index of the element to be removed
     * @return the removed value if present and available; otherwise {@code null}.
     */
    @Override
    public T remove(int index) {
        return getIfAvailableElseNull(removeLazy(index));
    }

    /**
     * Removes the first element of this list.
     *
     * <p>
     * The removed value is returned only if it had already been evaluated; otherwise {@code null} is returned
     * without forcing evaluation.
     *
     * @return the removed value if present and available; otherwise {@code null}.
     */
    @SuppressWarnings("java:S1161") // Can't override, removeFirst is not yet part of the Java 8 Collections API.
    public T removeFirst() {
        return remove(0);
    }

    /**
     * Removes the last element of this list.
     *
     * <p>
     * The removed value is returned only if it had already been evaluated; otherwise {@code null} is returned
     * without forcing evaluation.
     *
     * @return the removed value if present and available; otherwise {@code null}.
     */
    @SuppressWarnings("java:S1161") // Can't override, removeLast is not yet part of the Java 8 Collections API.
    public T removeLast() {
        return remove(size() - 1);
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        delegate.replaceAll(lazy -> lazy.map(operator));
    }

    @Override
    public void sort(Comparator<? super T> c) {
        delegate.sort(Comparator.comparing(Lazy::get, c));
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Spliterator<T> spliterator() {
        return new LazySpliterator<>(delegate.spliterator());
    }

    @Override
    public LazyIterator<T> iterator() {
        return listIterator();
    }

    @Override
    public LazyListIterator<T> listIterator() {
        return listIterator(0);
    }

    @Override
    public LazyListIterator<T> listIterator(int index) {
        return new LazyListIterator<>(delegate.listIterator(index));
    }

    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(Lazy.eager(o));
    }

    @Override
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(Lazy.eager(o));
    }

    @Override
    public boolean contains(Object value) {
        // First pass, only check the already-available values.
        boolean requiresSecondPass = false;
        for (Lazy<T> lazyValue : delegate) {
            if (!lazyValue.isAvailable()) {
                requiresSecondPass = true;
            } else if (Objects.equals(value, lazyValue.get())) {
                return true;
            }
        }
        // Second pass, also check the values that were not yet available in the first pass.
        return requiresSecondPass && delegate.contains(Lazy.eager(value));
    }

    @Override
    public boolean remove(Object value) {
        // First pass, only check the already-available values.
        boolean requiresSecondPass = false;
        for (Iterator<Lazy<T>> it = delegate.iterator(); it.hasNext(); ) {
            Lazy<T> lazyValue = it.next();
            if (!lazyValue.isAvailable()) {
                requiresSecondPass = true;
            } else if (Objects.equals(value, lazyValue.get())) {
                it.remove();
                return true;
            }
        }
        // Second pass, also check the values that were not yet available in the first pass.
        return requiresSecondPass && delegate.remove(Lazy.eager(value));
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return new LazyList<>(() -> delegate.subList(fromIndex, toIndex));
    }

    /**
     * Determine equality for this list.
     *
     * @param o object to be compared for equality with this list
     * @return Whether the specified object is another list containing the same values as this list.
     * @implNote To comply with the general {@link List} contract,
     * this involves eagerly evaluating lazy values until one is encountered that is not equal to the item from the other list.
     * Therefore, if the list is found to be equal, <em>all</em> its lazy entries will have been eagerly evaluated.
     */
    @Override
    public boolean equals(Object o) {
        // Determine equality according to the general contract of List.
        return super.equals(o);
    }

    /**
     * Calculate the hashcode for this list.
     *
     * @return The hashcode for this list.
     * @implNote To comply with the general {@link List} contract,
     * this involves eagerly evaluating <strong>all</strong> lazy values currently contained in the map.
     */
    @Override
    public int hashCode() {
        // Calculate hashcode for actual elements, not the delegate's (lazy) entries.
        return super.hashCode();
    }

    /**
     * String representation of this list.
     *
     * <p>
     * All values are represented by their {@link Lazy#toString()} representation.
     * This prevents unnecessary eager evaluation of values.
     *
     * @return Standard list representation of lazy values.
     */
    @Override
    public String toString() {
        return delegate.toString();
    }

    public Lazy<T>[] toLazyArray() {
        return toLazyArray(Lazy[]::new);
    }

    public Lazy<T>[] toLazyArray(Lazy<T>[] a) {
        return toLazyArray(s -> a);
    }

    public Lazy<T>[] toLazyArray(IntFunction<Lazy<T>[]> generator) {
        return delegate.toArray(generator.apply(size()));
    }

    /**
     * Iterate over all available values in this list.
     *
     * <p>
     * Lazy values that have not yet been evaluated will be skipped.
     *
     * @param action The action to perform on each available value.
     * @see #forEach(Consumer)
     * @see #forEachLazy(Consumer)
     */
    public void forEachAvailable(Consumer<? super T> action) {
        for (Lazy<T> lazy : delegate) {
            if (lazy.isAvailable()) {
                action.accept(lazy.get());
            }
        }
    }

    /**
     * Iterate over all lazy values in this list.
     *
     * @param action The action to perform on each lazy value.
     * @see #forEachAvailable(Consumer)
     * @see #forEach(Consumer)
     * @see Lazy
     */
    public void forEachLazy(Consumer<Lazy<? super T>> action) {
        for (Lazy<T> lazy : delegate) {
            action.accept(lazy);
        }
    }

    @Override
    public Stream<T> stream() {
        return streamLazy().map(Lazy::get);
    }

    public Stream<Lazy<T>> streamLazy() {
        return delegate.stream();
    }

    public Stream<T> streamAvailable() {
        return streamLazy().filter(Lazy::isAvailable).map(Lazy::get);
    }

    @Override
    public Stream<T> parallelStream() {
        return parallelStreamLazy().map(Lazy::get);
    }

    public Stream<Lazy<T>> parallelStreamLazy() {
        return delegate.parallelStream();
    }

    public Stream<T> parallelStreamAvailable() {
        return parallelStreamLazy().filter(Lazy::isAvailable).map(Lazy::get);
    }

    /**
     * Creates a {@code List<Lazy<T>>} copy of the given {@code Collection<T>}.
     *
     * <p>
     * If the source collection is already a {@code LazyList}, its internal lazy values are reused
     * without triggering evaluation. Otherwise, values are wrapped into eager {@link Lazy} instances.
     *
     * @param toCopy source collection to copy from.
     * @param <T>    type of the elements in the collection.
     * @return a new {@link ArrayList} containing lazy values that mirror the source contents.
     */
    @SuppressWarnings("unchecked") // It is safe to cast '? extends T' to T for reading.
    private static <T> List<Lazy<T>> copyToLazyList(Collection<? extends T> toCopy) {
        if (toCopy instanceof LazyList) { // Reuse lazy values if we can.
            return new ArrayList<>(((LazyList<T>) toCopy).delegate);
        }

        // Otherwise, convert the actual values to (eager) Lazy instances.
        List<Lazy<T>> copy = new ArrayList<>(toCopy.size());
        for (T value : toCopy) {
            copy.add(Lazy.eager(value));
        }
        return copy;
    }

    private static final class LazySpliterator<T> implements Spliterator<T> {
        private final Spliterator<Lazy<T>> delegateSpliterator;

        private LazySpliterator(Spliterator<Lazy<T>> delegate) {
            this.delegateSpliterator = delegate;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            return delegateSpliterator.tryAdvance(lazy -> action.accept(getNullSafe(lazy)));
        }

        @Override
        public Spliterator<T> trySplit() {
            Spliterator<Lazy<T>> result = delegateSpliterator.trySplit();
            return result == null ? null : new LazySpliterator<>(result);
        }

        @Override
        public long estimateSize() {
            return delegateSpliterator.estimateSize();
        }

        @Override
        public int characteristics() {
            return delegateSpliterator.characteristics();
        }
    }
}
