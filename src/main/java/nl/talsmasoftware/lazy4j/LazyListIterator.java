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

public final class LazyListIterator<T> implements LazyIterator<T>, ListIterator<T> {
    private final ListIterator<Lazy<T>> delegate;

    LazyListIterator(ListIterator<Lazy<T>> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    public Lazy<T> nextLazy() {
        return delegate.next();
    }

    @Override
    public T next() {
        return getNullSafe(nextLazy());
    }

    @Override
    public boolean hasPrevious() {
        return delegate.hasPrevious();
    }

    public Lazy<T> previousLazy() {
        return delegate.previous();
    }

    @Override
    public T previous() {
        return getNullSafe(previousLazy());
    }

    @Override
    public int nextIndex() {
        return delegate.nextIndex();
    }

    @Override
    public int previousIndex() {
        return delegate.previousIndex();
    }

    @Override
    public void remove() {
        delegate.remove();
    }

    public void setLazy(Supplier<? extends T> lazy) {
        delegate.set(Lazy.of(lazy));
    }

    @Override
    public void set(T value) {
        setLazy(Lazy.eager(value));
    }

    public void addLazy(Supplier<? extends T> lazy) {
        delegate.add(Lazy.of(lazy));
    }

    @Override
    public void add(T value) {
        addLazy(Lazy.eager(value));
    }

}
