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

import org.junit.jupiter.api.Test;

import java.util.ListIterator;

import static org.assertj.core.api.Assertions.assertThat;

class LazyListIteratorTest {
    @Test
    void next() {
        LazyList<String> lazyList = LazyList.create();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        for (ListIterator<String> it = lazyList.listIterator(); it.hasNext(); ) {
            String result = it.next();
            assertThat(result).isEqualTo(lazyList.get(it.previousIndex()));
        }
    }

    @Test
    void nextLazy() {
        LazyList<String> lazyList = LazyList.create();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        for (LazyListIterator<String> it = lazyList.listIterator(); it.hasNext(); ) {
            Lazy<String> result = it.nextLazy();
            assertThat(result.isAvailable()).isEqualTo(lazyList.getLazy(it.previousIndex()).isAvailable());
            assertThat(result).isEqualTo(lazyList.getLazy(it.previousIndex()));
        }
    }

    @Test
    void previous() {
        LazyList<String> lazyList = LazyList.create();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        for (ListIterator<String> it = lazyList.listIterator(lazyList.size()); it.hasPrevious(); ) {
            String result = it.previous();
            assertThat(result).isEqualTo(lazyList.get(it.nextIndex()));
        }
    }

    @Test
    void previousLazy() {
        LazyList<String> lazyList = LazyList.create();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        for (LazyListIterator<String> it = lazyList.listIterator(lazyList.size()); it.hasPrevious(); ) {
            Lazy<String> result = it.previousLazy();
            assertThat(result.isAvailable()).isEqualTo(lazyList.getLazy(it.nextIndex()).isAvailable());
            assertThat(result).isEqualTo(lazyList.getLazy(it.nextIndex()));
        }
    }

    @Test
    void remove() {
        LazyList<String> lazyList = LazyList.create();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        LazyListIterator<String> subject = lazyList.listIterator();
        subject.nextLazy();
        subject.nextLazy();
        subject.remove();

        assertThat(lazyList.streamAvailable()).isEmpty();
        assertThat(lazyList).hasSize(2).containsExactly("one", "three");
    }

    @Test
    void remove_afterPrevious() {
        LazyList<String> lazyList = LazyList.create();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        LazyListIterator<String> subject = lazyList.listIterator(lazyList.size());
        subject.previous();
        subject.previous();
        subject.remove();

        assertThat(lazyList.streamAvailable()).containsExactly("three");
        assertThat(lazyList).hasSize(2).containsExactly("one", "three");
    }

    @Test
    void setLazy() {
        LazyList<String> lazyList = LazyList.create();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        LazyListIterator<String> subject = lazyList.listIterator();
        subject.nextLazy();
        subject.nextLazy();
        subject.setLazy(() -> "other");

        assertThat(lazyList.streamAvailable()).isEmpty();
        assertThat(lazyList).containsExactly("one", "other", "three");
    }

    @Test
    void setLazy_afterPrevious() {
        LazyList<String> lazyList = LazyList.create();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        LazyListIterator<String> subject = lazyList.listIterator(lazyList.size());
        subject.previous();
        subject.previous();
        subject.setLazy(() -> "other");

        assertThat(lazyList.streamAvailable()).containsExactly("three");
        assertThat(lazyList).containsExactly("one", "other", "three");
    }

    @Test
    void set() {
        LazyList<String> lazyList = LazyList.create();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        LazyListIterator<String> subject = lazyList.listIterator();
        subject.nextLazy();
        subject.nextLazy();
        subject.set("other");

        assertThat(lazyList.streamAvailable()).containsExactly("other");
        assertThat(lazyList).containsExactly("one", "other", "three");
    }

    @Test
    void set_afterPrevious() {
        LazyList<String> lazyList = LazyList.create();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        LazyListIterator<String> subject = lazyList.listIterator(lazyList.size());
        subject.previous();
        subject.previous();
        subject.set("other");

        assertThat(lazyList.streamAvailable()).containsExactly("other", "three");
        assertThat(lazyList).containsExactly("one", "other", "three");
    }

    @Test
    void addLazy() {
        LazyList<String> lazyList = LazyList.create();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        LazyListIterator<String> subject = lazyList.listIterator();
        subject.nextLazy();
        subject.nextLazy();
        subject.addLazy(() -> "other");

        assertThat(lazyList.streamAvailable()).containsExactly("two");
        assertThat(lazyList).containsExactly("one", "two", "other", "three");
    }

    @Test
    void addLazy_afterPrevious() {
        LazyList<String> lazyList = LazyList.create();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        LazyListIterator<String> subject = lazyList.listIterator(lazyList.size());
        subject.previous();
        subject.previous();
        subject.addLazy(() -> "other");

        assertThat(lazyList.streamAvailable()).containsExactly("two", "three");
        assertThat(lazyList).containsExactly("one", "other", "two", "three");
    }

    @Test
    void add() {
        LazyList<String> lazyList = LazyList.create();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        LazyListIterator<String> subject = lazyList.listIterator();
        subject.nextLazy();
        subject.nextLazy();
        subject.add("other");

        assertThat(lazyList.streamAvailable()).containsExactly("two", "other");
        assertThat(lazyList).containsExactly("one", "two", "other", "three");
    }

    @Test
    void add_afterPrevious() {
        LazyList<String> lazyList = LazyList.create();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        LazyListIterator<String> subject = lazyList.listIterator(lazyList.size());
        subject.previous();
        subject.previous();
        subject.add("other");

        assertThat(lazyList.streamAvailable()).containsExactly("other", "two", "three");
        assertThat(lazyList).containsExactly("one", "other", "two", "three");
    }

}
