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

import java.util.ListIterator;
import java.util.function.Supplier;

import static nl.talsmasoftware.lazy4j.LazyUtils.getNullSafe;

/**
 * Lazy List iterator that allows access to the lazy, possibly unevaluated, value.
 *
 * @param <T> The type of elements being iterated.
 * @author Sjoerd Talsma
 * @since 2.0.3
 */
public final class LazyListIterator<T> implements LazyIterator<T>, ListIterator<T> {
    private final ListIterator<Lazy<T>> delegate;

    /**
     * Package-protected constructor.
     *
     * <p>
     * To be called by {@link LazyList#listIterator()}.
     *
     * @param delegate The delegate list iterator for the lazy values.
     */
    LazyListIterator(ListIterator<Lazy<T>> delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns {@code true} if there are more elements in the iteration.
     *
     * @return {@code true} if there are more elements in the iteration, otherwise {@code false}.
     */
    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    /**
     * Returns the next Lazy element in the iteration.
     *
     * @return The next Lazy element in the iteration.
     * @see #next()
     * @see #previousLazy()
     */
    public Lazy<T> nextLazy() {
        return delegate.next();
    }

    /**
     * Returns the next element in the iteration, eagerly evaluated.
     *
     * @return The next element in the iteration, eagerly evaluated.
     * @see #nextLazy()
     * @see #previousLazy()
     */
    @Override
    public T next() {
        return getNullSafe(nextLazy());
    }

    /**
     * Returns {@code true} if there are any previous elements in the iteration.
     *
     * @return {@code true} if there are previous elements in the iteration, otherwise {@code false}.
     */
    @Override
    public boolean hasPrevious() {
        return delegate.hasPrevious();
    }

    /**
     * Returns the previous Lazy element in the iteration.
     *
     * @return The previous Lazy element in the iteration.
     * @see #previous()
     * @see #nextLazy()
     */
    public Lazy<T> previousLazy() {
        return delegate.previous();
    }

    /**
     * Returns the previous element in the iteration, eagerly evaluated.
     *
     * @return The previous element in the iteration, eagerly evaluated.
     * @see #previousLazy()
     * @see #nextLazy()
     */
    @Override
    public T previous() {
        return getNullSafe(previousLazy());
    }

    /**
     * Returns the index of the element that would be returned by a subsequent call to next or nextLazy.
     *
     * @return The index of the next element or list size if the end of the list was reached.
     */
    @Override
    public int nextIndex() {
        return delegate.nextIndex();
    }

    /**
     * Returns the index of the element that would be returned by a subsequent call to previous or previousLazy.
     *
     * @return The index of the previous element or -1 if the beginning of the list was reached.
     */
    @Override
    public int previousIndex() {
        return delegate.previousIndex();
    }

    /**
     * Removes from the underlying collection the last element returned
     * by {@link #next()}, {@link #nextLazy()}, {@link #previous()} or {@link #previousLazy()}.
     */
    @Override
    public void remove() {
        delegate.remove();
    }

    /**
     * Replaces the last element returned
     * by {@link #next()} or {@link #nextLazy()}, {@link #previous()} or {@link #previousLazy()}
     * with the specified lazy element.
     *
     * <p>
     * The lazy element will only be evaluated when it is actually needed for the first time by the list.
     *
     * @param lazy Lazy supplier for the new element.
     */
    public void setLazy(Supplier<? extends T> lazy) {
        delegate.set(Lazy.of(lazy));
    }

    /**
     * Replaces the last element returned
     * by {@link #next()} or {@link #nextLazy()}, {@link #previous()} or {@link #previousLazy()}
     * with the specified element.
     *
     * @param value The new element.
     * @see #setLazy(Supplier)
     */
    @Override
    public void set(T value) {
        setLazy(Lazy.eager(value));
    }

    /**
     * Inserts the specified lazy element into the underlying collection immediately before the element that would be
     * returned by {@link #next()} or {@link #nextLazy()}, if any, and after the element that would be returned by
     * {@link #previous()} or {@link #previousLazy()}, if any.
     *
     * <p>
     * The lazy element will only be evaluated when it is actually needed for the first time by the list.
     *
     * @param lazy Lazy supplier for the new element.
     */
    public void addLazy(Supplier<? extends T> lazy) {
        delegate.add(Lazy.of(lazy));
    }

    /**
     * Inserts the specified element into the underlying collection immediately before the element that would be
     * returned by {@link #next()} or {@link #nextLazy()}, if any, and after the element that would be returned by
     * {@link #previous()} or {@link #previousLazy()}, if any.
     *
     * @param value The (eager) value for the new element.
     * @see #addLazy(Supplier)
     */
    @Override
    public void add(T value) {
        addLazy(Lazy.eager(value));
    }

}
