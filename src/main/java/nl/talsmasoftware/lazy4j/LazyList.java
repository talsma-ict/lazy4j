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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static nl.talsmasoftware.lazy4j.LazyUtils.getIfAvailableElseNull;
import static nl.talsmasoftware.lazy4j.LazyUtils.getNullSafe;

/**
 * A list that can store its values in a {@link Lazy} manner.
 *
 * <p>
 * This has the advantage that while providing 'standard' {@link List} features,
 * unused values do not need to be evaluated.
 *
 * <p>
 * The behaviour of this list depends on the delegate list it was {@link #LazyList(Supplier) initialized} with.<br>
 * If the delegate list is mutable, the lazy list will be mutable as well.<br>
 * If the delegate list is thread-safe, the lazy list will be thread-safe as well.<br>
 * If the lazy list is sorted, its purpose may be defeated because all values have to be evaluated eagerly for comparison.<br>
 * The {@link #LazyList() default} and {@link #LazyList(Collection) copy} constructors create a lazy list
 * that behaves like an {@link ArrayList}.<br>
 * If the backing list does <em>not</em> support {@code null} values, the lazy list <em>will</em> support them,
 * because the values are wrapped in a {@link Lazy} object, therefore will never be {@code null} in the backing map.
 *
 * @param <T> The type of values in the list.
 * @author Sjoerd Talsma
 * @see Lazy
 * @since 2.0.3
 */
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

    /**
     * Adds the specified element to the end of this list, without eagerly evaluating it.
     *
     * <p>
     * The element will be evaluated when it is needed for the first time.
     *
     * @param lazy The supplier for the new element, evaluated only if necessary.
     * @return {@code true} if the element was added to the list.
     * @throws UnsupportedOperationException if the backing list does not support adding elements.
     */
    public boolean addLazy(Supplier<? extends T> lazy) {
        return delegate.add(Lazy.of(lazy));
    }

    /**
     * Inserts the specified element at the specified position in this list, without eagerly evaluating it.
     *
     * <p>
     * Shifts the element currently at that position (if any) and any subsequent elements to the right
     * (adds one to their indices).
     *
     * <p>
     * The element will be evaluated when it is needed for the first time.
     *
     * @param index The index at which the specified element is to be inserted.
     * @param lazy  The supplier for the new element, evaluated only if necessary.
     * @throws UnsupportedOperationException if the backing list does not support adding elements.
     * @throws IndexOutOfBoundsException     if the index is out of range ({@code index < 0 || index > size()}).
     * @see #addLazy(Supplier)
     * @see #add(int, Object)
     */
    public void addLazy(int index, Supplier<? extends T> lazy) {
        delegate.add(index, Lazy.of(lazy));
    }

    /**
     * Inserts the specified element at the beginning of this list, without eagerly evaluating it.
     *
     * @param lazy The supplier for the new element, evaluated only if necessary.
     * @see #addLazy(Supplier)
     * @see #addLazy(int, Supplier)
     */
    public void addFirstLazy(Supplier<? extends T> lazy) {
        addLazy(0, lazy);
    }

    /**
     * Adds the specified element to the end of this list, without eagerly evaluating it.
     *
     * <p>
     * This is another variant of {@link #addLazy(Supplier)}.
     *
     * @param lazy The supplier for the new element, evaluated only if necessary.
     * @see #addLazy(Supplier)
     * @see #add(Object)
     */
    public void addLastLazy(Supplier<? extends T> lazy) {
        addLazy(lazy);
    }

    /**
     * Adds the lazy elements in the specified collection to the end of this list.
     *
     * <p>
     * All added lazy elements are evaluated only when they are first-needed.
     *
     * @param lazyValues The collection of lazy elements to be added to this list.
     * @return {@code true} if this list changed as a result of the call.
     * @see #addAllLazy(int, Collection)
     * @see #addAll(Collection)
     */
    public boolean addAllLazy(Collection<Supplier<? extends T>> lazyValues) {
        return addAllLazy(size(), lazyValues);
    }

    /**
     * Inserts the lazy elements in the specified collection into this list at the specified position.
     *
     * <p>
     * Shifts the element currently at that position (if any) and any elements to the right
     * (increases their indices).
     * The new elements will appear in this list in the order that they are returned by the specified collection's iterator.
     *
     * <p>
     * All inserted lazy elements are evaluated only when they are first-needed.
     *
     * @param index      The index at which to insert the first element from the specified collection.
     * @param lazyValues The collection of lazy elements to be inserted into this list.
     * @return {@code true} if this list changed as a result of the call.
     * @see #addAll(int, Collection)
     * @see #addAllLazy(Collection)
     */
    public boolean addAllLazy(int index, Collection<Supplier<? extends T>> lazyValues) {
        boolean changed = false;
        for (Supplier<? extends T> lazyValue : lazyValues) {
            addLazy(index++, lazyValue);
            changed = true;
        }
        return changed;
    }

    /**
     * Removes and returns the specified element from this list, without eagerly evaluating it.
     *
     * @param index The index of the element to remove.
     * @return The removed element.
     * @see #remove(int)
     */
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

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * <p>
     * The previous value at this index is returned, if it was already evaluated.
     * Otherwise {@code null} is returned without forcing evaluation.
     *
     * @param index   The index of the element to set the new value for. This index must be within the bounds of the list.
     * @param element The new value for the element at the specified index.
     * @return The previous value at the specified index, if present and available. Otherwise {@code null}.
     * @see #setLazy(int, Supplier)
     */
    @Override
    public T set(int index, T element) {
        return getIfAvailableElseNull(setLazy(index, Lazy.eager(element)));
    }

    /**
     * Adds the specified element at the specified position in this list.
     *
     * @return {@code true} if the element was added to the list.
     * @see #addLazy(Supplier)
     */
    @Override
    public boolean add(T value) {
        return addLazy(Lazy.eager(value));
    }

    /**
     * Inserts the specified element at the specified position in this list.
     *
     * @param index   The index at which the specified element is to be inserted.
     * @param element The element to be inserted.
     * @see #addLazy(int, Supplier)
     */
    @Override
    public void add(int index, T element) {
        addLazy(index, Lazy.eager(element));
    }

    /**
     * Inserts the specified values at the specified position in this list.
     *
     * @param index  The index at which the specified element is to be inserted.
     * @param values The values to be inserted.
     * @return {@code true} if the values were inserted in the list.
     * @see #addAllLazy(int, Collection)
     * @see #addAllLazy(Collection)
     */
    @Override
    public boolean addAll(int index, Collection<? extends T> values) {
        boolean changed = false;
        for (T value : values) {
            add(index++, value);
            changed = true;
        }
        return changed;
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
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
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
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return remove(size() - 1);
    }

    /**
     * Replaces each element of this list with the result of applying the operator to that element.
     *
     * <p>
     * The operator will be applied <em>lazily</em> when the result is needed for the first time.
     * So even if the underlying <em>value</em> is already available, the operator will not be applied immediately.
     *
     * @param operator The operator to lazily apply to each element.
     * @see Lazy#map(Function)
     */
    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        delegate.replaceAll(lazy -> lazy.map(operator));
    }

    /**
     * Sorts this list in accordance with the specified {@link Comparator}.
     *
     * <p>
     * Sorting will eagerly evaluate <em>all</em> lazy values currently contained in the list.
     *
     * @param comparator The comparator comparing the individual values to each-other.
     */
    @Override
    public void sort(Comparator<? super T> comparator) {
        delegate.sort(Comparator.comparing(Lazy::get, comparator));
    }

    /**
     * The number of elements in this list.
     *
     * @return The number of elements in this list.
     */
    @Override
    public int size() {
        return delegate.size();
    }

    /**
     * Whether the list is empty or not.
     *
     * @return {@code true} if the list is empty, {@code false} otherwise.
     */
    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    /**
     * Removes <em>all</em> elements from the list, regardless of whether they have already been evaluated.
     */
    @Override
    public void clear() {
        delegate.clear();
    }

    /**
     * A lazy spliterator over the elements in this list.
     *
     * <p>
     * The spliterator will have the same behaviour characteristics as the spliterator of the underlying list.
     *
     * @return a lazy Spliterator over the elements in this list
     * @see List#spliterator()
     */
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

    /**
     * Attempts to find the specified value in this list.
     *
     * <p>
     * Values are evaluated in-order until the first match is found.
     * This means that <em>all</em> lazy values 'left' of the found element
     * will have been eagerly evaluated.<br>
     * If no matching element is found, the method returns {@code -1}
     * and <em>all</em> elements will have been eagerly evaluated.
     *
     * @param value the element to search for.
     * @return The index of the first matching element, or {@code -1} if no match was found.
     * @implNote Note that getting the index of an element may evaluate more elements
     * than calling {@link #contains(Object)} does, because the latter uses a two-pass algorithm
     * where the first pass checks the available values only.
     * @see #contains(Object)
     * @see #streamAvailable()
     * @see #streamLazy()
     */
    @Override
    public int indexOf(Object value) {
        return delegate.indexOf(Lazy.eager(value));
    }

    /**
     * Attempts to find the last occurrence of the specified value in this list.
     *
     * <p>
     * Values are evaluated in reverse order until the first match is found.
     * This means that <em>all</em> lazy values to the 'right' of the found element
     * will have been eagerly evaluated.<br>
     * If no matching element is found, the method returns {@code -1}
     * and <em>all</em> elements will have been eagerly evaluated.
     *
     * @param value the element to search for.
     * @return The index of the last matching element, or {@code -1} if no match was found.
     * @implNote Note that getting the last index of an element may evaluate more elements
     * than calling {@link #contains(Object)} does, because the latter uses a two-pass algorithm
     * where the first pass checks the available values only.
     * @see #contains(Object)
     * @see #streamAvailable()
     * @see #streamLazy()
     */
    @Override
    public int lastIndexOf(Object value) {
        return delegate.lastIndexOf(Lazy.eager(value));
    }

    /**
     * Whether the specified value is contained in this list.
     *
     * @param value element whose presence in this collection is to be tested.
     * @param value The value to check for.
     * @return {@code true} if this collection contains the specified element.
     * @return {@code true} if the list contains the specified value, {@code false} otherwise.
     * @implNote This implementation uses a two-pass algorithm where the first pass checks available values only.
     * This avoids unnecessary eager evaluation of lazy values when the searched value is already evaluated.
     * If the method returns {@code false}, <em>all</em> lazy values have been evaluated.
     * @see #streamAvailable()
     * @see #streamLazy()
     * @see #containsAll(Collection)
     */
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

    /**
     * Remove an instance of the specified value from this list.
     *
     * @param value element to be removed from this collection, if present
     * @return {@code true} if the requested element was removed from this collection.
     * @implNote This implementation uses a two-pass algorithm where the first pass checks available values only.
     * This avoids unnecessary eager evaluation of lazy values when the searched value is already evaluated.
     * If the method returns {@code false}, <em>all</em> lazy values have been evaluated.
     */
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

    /**
     * Create a lazy sublist of this list.
     *
     * <p>
     * The sublist will be backed by the same underlying list,
     * so changes to the original list will be reflected in the sublist and vice versa.
     *
     * <p>
     * New {@link Lazy} values {@link #addLazy(Supplier) added} to the sublist will be shared by both lists,
     * still evaluating them only once, when needed.
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex   high endpoint (exclusive) of the subList
     * @return A new {@link LazyList} containing the elements between the specified fromIndex (inclusive) and toIndex (exclusive).
     */
    @Override
    public LazyList<T> subList(int fromIndex, int toIndex) {
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

    /**
     * Returns the elements in this list as an array of lazy values.
     *
     * @return An array of all lazy values.
     */
    public Lazy<T>[] toLazyArray() {
        return toLazyArray(Lazy[]::new);
    }

    /**
     * Returns an array of all lazy values in the list.
     *
     * <p>
     * This includes both evaluated and not-yet evaluated values.
     *
     * @param array The array to return the elements in
     *              (if it is big enough; otherwise, a new array of the same runtime type created).
     * @return Array containing all lazy values from this list.
     * @see Lazy
     * @see #streamLazy()
     */
    public Lazy<T>[] toLazyArray(Lazy<T>[] array) {
        return toLazyArray(s -> array);
    }

    /**
     * Returns an array of all lazy values in the list.
     *
     * <p>
     * This includes both evaluated and not-yet evaluated values.
     *
     * @param generator Generator for the array to return the elements in
     *                  (if it is big enough; otherwise, a new array of the same runtime type created).
     * @return Array containing all lazy values from this list.
     * @see Lazy
     * @see #streamLazy()
     */
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
            lazy.ifAvailable(action);
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
    public void forEachLazy(Consumer<Lazy<T>> action) {
        for (Lazy<T> lazy : delegate) {
            action.accept(lazy);
        }
    }

    /**
     * A sequential Stream of the evaluated values in this lazy List.
     *
     * <p>
     * Iterated values are evaluated eagerly. See {@link #streamAvailable()} or {@link #streamLazy()}
     * for streams that do not eagerly evaluate lazy values.
     *
     * <p>
     * This is the equivalent of calling {@code streamLazy().map(Lazy::get)}.
     *
     * @return Sequential stream of all values in this list.
     * @see #streamAvailable()
     * @see #streamLazy()
     * @see #parallelStreamLazy()
     * @see #parallelStreamAvailable()
     */
    @Override
    public Stream<T> stream() {
        return streamLazy().map(LazyUtils::getNullSafe);
    }

    /**
     * Sequential Stream of the lazy values in this List.
     *
     * <p>
     * This returns both evaluated and not-yet evaluated lazy values.
     *
     * @return Sequential stream of all lazy values in this list.
     * @see #streamAvailable()
     * @see #parallelStreamLazy()
     * @see #parallelStreamAvailable()
     */
    public Stream<Lazy<T>> streamLazy() {
        return delegate.stream();
    }

    /**
     * Sequential Stream of the available (already-evaluated) values in this list, skipping not-yet evaluated lazy values.
     *
     * <p>
     * This is the equivalent of calling<br>
     * {@code streamLazy().filter(Lazy::isAvailable).map(Lazy::get)}.
     *
     * @return Sequential stream of the available values.
     * @see #streamLazy()
     * @see #parallelStreamAvailable()
     * @see #parallelStreamLazy()
     */
    public Stream<T> streamAvailable() {
        return streamLazy().filter(LazyUtils::isAvailable).map(Lazy::get);
    }

    /**
     * A parallel Stream of the evaluated values in this lazy List.
     *
     * <p>
     * If the backing list does not support parallel streams, a sequential stream is returned.
     *
     * <p>
     * Iterated values are evaluated eagerly. See {@link #parallelStreamAvailable()} or {@link #parallelStreamLazy()}
     * for streams that do not eagerly evaluate lazy values.
     *
     * <p>
     * This is the equivalent of calling {@code parallelStreamLazy().map(Lazy::get)}.
     *
     * @return Parallel stream of all values in this list.
     * @see #parallelStreamAvailable()
     * @see #parallelStreamLazy()
     * @see #streamLazy()
     * @see #streamAvailable()
     */
    @Override
    public Stream<T> parallelStream() {
        return parallelStreamLazy().map(LazyUtils::getNullSafe);
    }

    /**
     * Parallel Stream of the lazy values in this List.
     *
     * <p>
     * If the backing list does not support parallel streams, a sequential stream is returned.
     *
     * <p>
     * This returns both evaluated and not-yet evaluated lazy values.
     *
     * @return Parallel stream of all lazy values in this list.
     * @see #parallelStreamAvailable()
     * @see #streamLazy()
     * @see #streamAvailable()
     */
    public Stream<Lazy<T>> parallelStreamLazy() {
        return delegate.parallelStream();
    }

    /**
     * Parallel Stream of the available (already-evaluated) values in this list, skipping not-yet evaluated lazy values.
     *
     * <p>
     * If the backing list does not support parallel streams, a sequential stream is returned.
     *
     * <p>
     * This is the equivalent of calling<br>
     * {@code parallelStreamLazy().filter(Lazy::isAvailable).map(Lazy::get)}.
     *
     * @return Parallel stream of the available values.
     * @see #streamLazy()
     * @see #parallelStreamAvailable()
     * @see #parallelStreamLazy()
     */
    public Stream<T> parallelStreamAvailable() {
        return parallelStreamLazy().filter(LazyUtils::isAvailable).map(Lazy::get);
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

    /**
     * Lazy Spliterator implementation for {@link LazyList}.
     *
     * @param <T> The type of elements in the list.
     */
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
