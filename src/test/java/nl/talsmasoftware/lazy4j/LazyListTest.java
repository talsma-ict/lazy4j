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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LazyListTest {

    @Test
    void getLazy() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyList<String> subject = new LazyList<>(() -> Arrays.asList(lazy, Lazy.of(() -> "other")));

        assertThat(subject.getLazy(0).isAvailable()).isFalse();
        assertThat(lazy.isAvailable()).isFalse();

        assertThat(subject).element(0).isEqualTo("test");
        assertThat(lazy.isAvailable()).isTrue();
        assertThat(subject.getLazy(0).isAvailable()).isTrue();

        assertThat(subject.getLazy(0)).isEqualTo(Lazy.eager("test"));
        assertThat(subject.getLazy(1)).isEqualTo(Lazy.eager("other"));
    }

    @Test
    void getLazy_indexOutOfBounds() {
        LazyList<String> subject = new LazyList<>(() -> Arrays.asList(Lazy.of(() -> "test"), Lazy.of(() -> "other")));
        assertThatThrownBy(() -> subject.getLazy(2)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void getFirstLazy() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyList<String> subject = new LazyList<>(() -> Arrays.asList(lazy, Lazy.of(() -> "other")));

        assertThat(subject.getFirstLazy().isAvailable()).isFalse();
        lazy.get();
        assertThat(subject.getFirstLazy().isAvailable()).isTrue();
        assertThat(subject.getFirstLazy()).isEqualTo(Lazy.eager("test"));
    }

    @Test
    void getFirstLazy_emptyList() {
        LazyList<String> subject = new LazyList<>();
        assertThatThrownBy(subject::getFirstLazy).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void getLastLazy() {
        Lazy<String> lazy = Lazy.of(() -> "other");
        LazyList<String> subject = new LazyList<>(() -> Arrays.asList(Lazy.of(() -> "test"), lazy));

        assertThat(subject.getLastLazy().isAvailable()).isFalse();
        lazy.get();
        assertThat(subject.getLastLazy().isAvailable()).isTrue();
        assertThat(subject.getLastLazy()).isEqualTo(Lazy.eager("other"));
    }

    @Test
    void getLastLazy_emptyList() {
        LazyList<String> subject = new LazyList<>();
        assertThatThrownBy(subject::getLastLazy).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void get() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyList<String> subject = new LazyList<>(() -> singletonList(lazy));

        assertThat(subject.get(0)).isEqualTo("test");
        assertThat(lazy.isAvailable()).isTrue();
    }

    @Test
    void setLazy() {
        // given
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "old");

        // when
        Lazy<String> result = subject.setLazy(0, lazy);

        // then
        assertThat(lazy.isAvailable()).isFalse();
        assertThat(result.isAvailable()).isFalse();

        assertThat(subject).hasSize(1).element(0).isEqualTo("test");
        assertThat(lazy.isAvailable()).isTrue();
        assertThat(result.isAvailable()).isFalse();

        assertThat(result.get()).isEqualTo("old");
        assertThat(result.isAvailable()).isTrue();
    }

    @Test
    void setLazy_indexOutOfBounds() {
        LazyList<String> subject = new LazyList<>();
        assertThatThrownBy(() -> subject.setLazy(2, () -> "test")).isInstanceOf(IndexOutOfBoundsException.class);
        assertThat(subject).isEmpty();
    }

    @Test
    void set() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(lazy);

        assertThat(subject.set(0, "other")).isNull();
        assertThat(lazy.isAvailable()).isFalse();

        assertThat(subject.set(0, "new")).isEqualTo("other"); // previous value was already available.
        assertThat(subject).hasSize(1).element(0).isEqualTo("new");
    }

    @Test
    void set_indexOutOfBounds() {
        LazyList<String> subject = new LazyList<>();
        assertThatThrownBy(() -> subject.set(1, "test")).isInstanceOf(IndexOutOfBoundsException.class);
        assertThat(subject).isEmpty();
    }

    @Test
    void addLazy() {
        LazyList<String> subject = new LazyList<>();

        assertThat(subject.addLazy(() -> "test")).isTrue();
        assertThat(subject.addLazy(() -> "test")).isTrue();
        assertThat(subject).hasSize(2);
        assertThat(subject.getLazy(0).isAvailable()).isFalse();
        assertThat(subject.getLazy(1).isAvailable()).isFalse();
    }

    @Test
    void addLazy_unmodifiable() {
        LazyList<String> subject = new LazyList(Collections::emptyList);

        assertThatThrownBy(() -> subject.addLazy(() -> "test"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void addLazy_index() {
        LazyList<String> subject = new LazyList<>(singletonList("test"));

        subject.addLazy(1, () -> "last");
        subject.addLazy(0, () -> "first");

        assertThat(subject).hasSize(3);
        assertThat(subject.getLazy(0).isAvailable()).isFalse();
        assertThat(subject.getLazy(1).isAvailable()).isTrue();
        assertThat(subject.getLazy(2).isAvailable()).isFalse();
        assertThat(subject).containsExactly("first", "test", "last");
    }

    @Test
    void addLazy_index_indexOutOfBounds() {
        LazyList<String> subject = new LazyList<>(singletonList("test"));

        assertThatThrownBy(() -> subject.addLazy(2, () -> "last"))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void addFirstLazy() {
        LazyList<String> subject = new LazyList<>();

        subject.addFirstLazy(() -> "first");
        subject.addFirstLazy(() -> "second");

        assertThat(subject).hasSize(2);
        assertThat(subject.getLazy(0).isAvailable()).isFalse();
        assertThat(subject.getLazy(1).isAvailable()).isFalse();
        assertThat(subject).containsExactly("second", "first");
    }

    @Test
    void addFirstLazy_unmodifiable() {
        LazyList<String> subject = new LazyList(Collections::emptyList);
        assertThatThrownBy(() -> subject.addFirstLazy(() -> "test"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void addLastLazy() {
        LazyList<String> subject = new LazyList<>();

        subject.addLastLazy(() -> "first");
        subject.addLastLazy(() -> "second");

        assertThat(subject).hasSize(2);
        assertThat(subject.getLazy(0).isAvailable()).isFalse();
        assertThat(subject.getLazy(1).isAvailable()).isFalse();
        assertThat(subject).containsExactly("first", "second");
    }

    @Test
    void addLastLazy_unmodifiable() {
        LazyList<String> subject = new LazyList(Collections::emptyList);
        assertThatThrownBy(() -> subject.addLastLazy(() -> "test"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void addAllLazy_empty() {
        LazyList<String> subject = new LazyList<>();
        assertThat(subject.addAllLazy(Collections.emptyList())).isFalse();
        assertThat(subject).isEmpty();
    }

    @Test
    void addAllLazy_unmodifiable() {
        LazyList<String> subject = new LazyList(Collections::emptyList);
        assertThatThrownBy(() -> subject.addAllLazy(singletonList(() -> "test")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void addAllLazy_list() {
        LazyList<String> subject = new LazyList<>();

        assertThat(subject.addAllLazy(Arrays.asList(() -> "test", () -> "other"))).isTrue();

        assertThat(subject).hasSize(2);
        assertThat(subject.getLazy(0).isAvailable()).isFalse();
        assertThat(subject.getLazy(1).isAvailable()).isFalse();
        assertThat(subject).containsExactly("test", "other");
    }

    @Test
    void addAllLazy_index() {
        LazyList<String> subject = new LazyList<>(singletonList("test"));

        assertThat(subject.addAllLazy(0, Arrays.asList(() -> "first", () -> "second"))).isTrue();
        assertThat(subject.addAllLazy(3, Arrays.asList(() -> "third", () -> "fourth"))).isTrue();
        assertThat(subject).hasSize(5);
        assertThat(subject.getLazy(0).isAvailable()).isFalse();
        assertThat(subject.getLazy(1).isAvailable()).isFalse();
        assertThat(subject.getLazy(2).isAvailable()).isTrue();
        assertThat(subject.getLazy(3).isAvailable()).isFalse();
        assertThat(subject.getLazy(4).isAvailable()).isFalse();
        assertThat(subject).containsExactly("first", "second", "test", "third", "fourth");
    }

    @Test
    void addAllLazy_index_indexOutOfBounds() {
        LazyList<String> subject = new LazyList<>(singletonList("test"));
        assertThatThrownBy(() -> subject.addAllLazy(2, singletonList(() -> "last")))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void add() {
        LazyList<String> subject = new LazyList<>();

        assertThat(subject.add("test")).isTrue();
        assertThat(subject.add("other")).isTrue();

        assertThat(subject).hasSize(2);
        assertThat(subject.getLazy(0).isAvailable()).isTrue();
        assertThat(subject.getLazy(1).isAvailable()).isTrue();
        assertThat(subject).containsExactly("test", "other");
    }

    @Test
    void add_unmodifiable() {
        LazyList<String> subject = new LazyList(Collections::emptyList);
        assertThatThrownBy(() -> subject.add("test"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void add_index() {
        LazyList<String> subject = new LazyList<>(singletonList("test"));

        subject.add(0, "first");
        subject.add(1, "second");

        assertThat(subject).hasSize(3).containsExactly("first", "second", "test");
    }

    @Test
    void add_index_indexOutOfBounds() {
        LazyList<String> subject = new LazyList<>(singletonList("test"));
        assertThatThrownBy(() -> subject.add(2, "last"))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void addAll_empty() {
        LazyList<String> subject = new LazyList<>();

        assertThat(subject.addAll(Collections.emptyList())).isFalse();
        assertThat(subject).isEmpty();
    }

    @Test
    void addAll_unmodifiable() {
        LazyList<String> subject = new LazyList(Collections::emptyList);

        assertThatThrownBy(() -> subject.addAll(singletonList("test")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void addAll() {
        LazyList<String> subject = new LazyList<>();

        assertThat(subject.addAll(Arrays.asList("test", "other"))).isTrue();

        assertThat(subject).hasSize(2).containsExactly("test", "other");
    }

    @Test
    void addAll_index() {
        LazyList<String> subject = new LazyList<>(singletonList("test"));

        assertThat(subject.addAll(0, Arrays.asList("first", "second"))).isTrue();
        assertThat(subject.addAll(3, Arrays.asList("third", "fourth"))).isTrue();

        assertThat(subject).hasSize(5).containsExactly("first", "second", "test", "third", "fourth");
    }

    @Test
    void addAll_index_indexOutOfBounds() {
        LazyList<String> subject = new LazyList<>(singletonList("test"));
        assertThatThrownBy(() -> subject.addAll(2, singletonList("last")))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void removeLazy() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(lazy);
        subject.addLazy(() -> "other");

        Lazy<String> removed = subject.removeLazy(0);

        assertThat(removed.isAvailable()).isFalse();
        assertThat(lazy.get()).isEqualTo("test");
        assertThat(subject).hasSize(1).containsExactly("other");
    }

    @Test
    void removeLazy_indexOutOfBounds() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "test");

        assertThatThrownBy(() -> subject.removeLazy(2)).isInstanceOf(IndexOutOfBoundsException.class);

        assertThat(subject).hasSize(1);
        assertThat(subject.getLazy(0).isAvailable()).isFalse();
    }

    @Test
    void removeLazy_unmodifiable() {
        LazyList<String> subject = new LazyList(() -> singletonList(Lazy.of(() -> "test")));

        assertThatThrownBy(() -> subject.removeLazy(0))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(subject).hasSize(1);
        assertThat(subject.getLazy(0).isAvailable()).isFalse();
        assertThat(subject).containsExactly("test");
    }

    @Test
    void remove_not_evaluated() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(lazy);

        assertThat(subject.remove(0)).isNull();

        assertThat(subject).isEmpty();
        assertThat(lazy.isAvailable()).isFalse();
    }

    @Test
    void remove_already_evaluated() {
        LazyList<String> subject = new LazyList<>(singletonList("test"));

        assertThat(subject.remove(0)).isEqualTo("test");

        assertThat(subject).isEmpty();
    }

    @Test
    void remove_indexOutOfBounds() {
        LazyList<String> subject = new LazyList<>();
        assertThatThrownBy(() -> subject.remove(2))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void remove_unmodifiable() {
        LazyList<String> subject = new LazyList<>(() -> singletonList(Lazy.eager("test")));
        assertThatThrownBy(() -> subject.remove(0))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void removeFirst_not_evaluated() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(lazy);
        subject.addLazy(() -> "other");

        assertThat(subject.removeFirst()).isNull();

        assertThat(subject).hasSize(1).containsExactly("other");
        assertThat(lazy.isAvailable()).isFalse();
    }

    @Test
    void removeFirst_already_evaluated() {
        LazyList<String> subject = new LazyList<>(Arrays.asList("test", "other"));

        assertThat(subject.removeFirst()).isEqualTo("test");

        assertThat(subject).hasSize(1).containsExactly("other");
    }

    @Test
    void removeFirst_unmodifiable() {
        LazyList<String> subject = new LazyList<>(() -> Arrays.asList(Lazy.eager("test"), Lazy.eager("other")));
        assertThatThrownBy(() -> subject.removeFirst())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void removeFirst_empty() {
        LazyList<String> subject = new LazyList<>();
        assertThatThrownBy(subject::removeFirst).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void removeLast_not_evaluated() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "first");
        subject.addLazy(lazy);

        assertThat(subject.removeLast()).isNull();

        assertThat(subject).hasSize(1).containsExactly("first");
        assertThat(lazy.isAvailable()).isFalse();
    }

    @Test
    void removeLast_already_evaluated() {
        LazyList<String> subject = new LazyList<>(Arrays.asList("test", "other"));

        assertThat(subject.removeLast()).isEqualTo("other");

        assertThat(subject).hasSize(1).containsExactly("test");
    }

    @Test
    void removeLast_unmodifiable() {
        LazyList<String> subject = new LazyList<>(() -> Arrays.asList(Lazy.eager("test"), Lazy.eager("other")));
        assertThatThrownBy(() -> subject.removeLast())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void removeLast_empty() {
        LazyList<String> subject = new LazyList<>();
        assertThatThrownBy(subject::removeLast).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void replaceAll_empty() {
        LazyList<String> subject = new LazyList<>();

        subject.replaceAll(s -> s + "suffix");

        assertThat(subject).isEmpty();
    }

    @Test
    void replaceAll_unmodifiable() {
        LazyList<String> subject = new LazyList<>(() -> singletonList(Lazy.eager("test")));
        assertThatThrownBy(() -> subject.replaceAll(s -> s + "suffix"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void replaceAll_not_yet_evaluated() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "test");
        subject.addLazy(() -> "other");

        subject.replaceAll(s -> s + "suffix");

        assertThat(subject.getLazy(0).isAvailable()).isFalse();
        assertThat(subject.getLazy(1).isAvailable()).isFalse();
        assertThat(subject).hasSize(2).containsExactly("testsuffix", "othersuffix");
    }

    @Test
    void replaceAll_already_evaluated() {
        LazyList<String> subject = new LazyList<>(Arrays.asList("test", "other"));

        subject.replaceAll(s -> s + "suffix");

        // operation must be lazily evaluated even if value is already available.
        assertThat(subject.getLazy(0).isAvailable()).isFalse();
        assertThat(subject.getLazy(1).isAvailable()).isFalse();
        assertThat(subject).hasSize(2).containsExactly("testsuffix", "othersuffix");
    }

    @Test
    void sort() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "first");
        subject.addLazy(() -> "test");
        subject.addLazy(() -> "other");

        subject.sort(Comparator.naturalOrder());

        subject.streamLazy().forEach(lazy -> assertThat(lazy.isAvailable()).isTrue());
        assertThat(subject).containsExactly("first", "other", "test");
    }

    @Test
    void sort_unmodifiable() {
        LazyList<String> subject = new LazyList<>(() -> unmodifiableList(Arrays.asList(Lazy.of(() -> "test"), Lazy.of(() -> "other"))));
        assertThatThrownBy(() -> subject.sort(Comparator.naturalOrder()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(subject).containsExactly("test", "other");
    }

    @Test
    void size() {
        LazyList<String> subject = new LazyList<>();
        assertThat(subject).hasSize(0).isEqualTo(emptyList()).isEmpty();

        subject.addLazy(() -> "test");
        assertThat(subject).hasSize(1).isNotEmpty();

        subject.addLazy(() -> "other");
        assertThat(subject).hasSize(2).containsExactly("test", "other");
    }

    @Test
    void isEmpty() {
        LazyList<String> subject = new LazyList<>();
        assertThat(subject).isEmpty();

        subject.addLazy(() -> null);
        assertThat(subject).isNotEmpty().hasSize(1);

        subject.removeLazy(0);
        assertThat(subject).isEmpty();
    }

    @Test
    void clear() {
        Lazy<String> test = Lazy.of(() -> "test");
        Lazy<String> other = Lazy.of(() -> "other");
        Lazy<String> eager = Lazy.eager("eager");
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "test");
        subject.addLazy(() -> "other");
        subject.addLazy(eager);

        subject.clear();

        assertThat(subject).isEmpty();
        assertThat(test.isAvailable()).isFalse();
        assertThat(other.isAvailable()).isFalse();
        assertThat(eager.isAvailable()).isTrue();
    }

    @Test
    void indexOf_empty() {
        LazyList<String> subject = new LazyList<>();
        assertThat(subject.indexOf("test")).isEqualTo(-1);
    }

    @Test
    void indexOf() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "zero");
        subject.addLazy(() -> "one");
        subject.addLazy(() -> "two");
        subject.addLazy(() -> "three");

        assertThat(subject.indexOf("one")).isEqualTo(1);
        assertThat(subject.streamLazy().map(Lazy::isAvailable)).containsExactly(true, true, false, false);
    }

    @Test
    void indexOf_null() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "test");
        subject.addLazy(() -> null);
        subject.addLazy(() -> "other");

        assertThat(subject.indexOf(null)).isEqualTo(1);
        assertThat(subject.streamLazy().map(Lazy::isAvailable)).containsExactly(true, true, false);
    }

    @Test
    void indexOf_not_found() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "zero");
        subject.addLazy(() -> "one");
        subject.addLazy(() -> "two");

        assertThat(subject.indexOf("three")).isEqualTo(-1);
        subject.streamLazy().forEach(lazy -> assertThat(lazy.isAvailable()).isTrue());
    }

    @Test
    void lastIndexOf_empty() {
        LazyList<String> subject = new LazyList<>();
        assertThat(subject.lastIndexOf("test")).isEqualTo(-1);
    }

    @Test
    void lastIndexOf() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "zero");
        subject.addLazy(() -> "one");
        subject.addLazy(() -> "two");

        assertThat(subject.lastIndexOf("one")).isEqualTo(1);
        assertThat(subject.streamLazy().map(Lazy::isAvailable)).containsExactly(false, true, true);
    }

    @Test
    void lastIndexOf_null() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "test");
        subject.addLazy(() -> null);
        subject.addLazy(() -> "other");

        assertThat(subject.lastIndexOf(null)).isEqualTo(1);
        assertThat(subject.streamLazy().map(Lazy::isAvailable)).containsExactly(false, true, true);
    }

    @Test
    void lastIndexOf_not_found() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "zero");
        subject.addLazy(() -> "one");
        subject.addLazy(() -> "two");

        assertThat(subject.lastIndexOf("three")).isEqualTo(-1);
        subject.streamLazy().forEach(lazy -> assertThat(lazy.isAvailable()).isTrue());
    }

    @Test
    void contains_foundInFirstPass() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "test");
        subject.add("eager");
        subject.addLazy(() -> "other");

        assertThat(subject.contains("eager")).isTrue();
        assertThat(subject.streamLazy().map(Lazy::isAvailable)).containsExactly(false, true, false);
    }

    @Test
    void contains_foundInSecondPass() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "test");
        subject.add("eager");
        subject.addLazy(() -> "other");

        assertThat(subject.contains("test")).isTrue();
        assertThat(subject.streamLazy().map(Lazy::isAvailable)).containsExactly(true, true, false);
    }

    @Test
    void contains_notFound() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "test");
        subject.add("eager");
        subject.addLazy(() -> "other");

        assertThat(subject.contains(null)).isFalse();
        assertThat(subject.streamLazy().map(Lazy::isAvailable)).containsExactly(true, true, true);
    }

    @Test
    void contains_null() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> null);
        subject.add("eager");
        subject.addLazy(() -> "other");

        assertThat(subject.contains(null)).isTrue();
        assertThat(subject.streamLazy().map(Lazy::isAvailable)).containsExactly(true, true, false);
    }

    @Test
    void contains_secondPassOnlyForUnevaluatedLazyValues() {
        AtomicBoolean backingContainsCalled = new AtomicBoolean(false);
        LazyList<String> subject = new LazyList<>(() -> new ArrayList<Lazy<String>>() {
            @Override
            public boolean contains(Object o) {
                backingContainsCalled.set(true);
                return super.contains(o);
            }
        });
        subject.addAll(Arrays.asList("all", "eager", "values"));

        assertThat(subject.contains("needle")).isFalse();
        assertThat(backingContainsCalled).isFalse();
    }

    @Test
    void remove_foundInFirstPass() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "other");
        subject.addLazy(() -> "test");
        subject.addLazy(() -> "other");
        subject.add("test"); // eager

        assertThat(subject.remove("test")).isTrue();

        // First-pass should remove the last entry first.
        assertThat(subject).hasSize(3);
        assertThat(subject.streamLazy().map(Lazy::isAvailable)).containsExactly(false, false, false);

        // Next attempt requires second pass.
        assertThat(subject.remove("test")).isTrue();
        assertThat(subject).hasSize(2);
        assertThat(subject.streamLazy().map(Lazy::isAvailable)).containsExactly(true, false);

        // When nothing was removed, all remaining values were eagerly evaluated.
        assertThat(subject.remove("test")).isFalse();
        assertThat(subject).hasSize(2);
        assertThat(subject.streamLazy().map(Lazy::isAvailable)).containsExactly(true, true);
        assertThat(subject).containsExactly("other", "other");
    }

    @Test
    void remove_secondPassOnlyForUnevaluatedLazyValues() {
        AtomicBoolean backingContainsCalled = new AtomicBoolean(false);
        LazyList<String> subject = new LazyList<>(() -> new ArrayList<Lazy<String>>() {
            @Override
            public boolean contains(Object o) {
                backingContainsCalled.set(true);
                return super.contains(o);
            }
        });
        subject.addAll(Arrays.asList("all", "eager", "values"));

        assertThat(subject.remove("needle")).isFalse();
        assertThat(backingContainsCalled).isFalse();
    }

    @Test
    void sublist() {
        // given
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "test");
        subject.addLazy(() -> "other");

        // when
        LazyList<String> sublist = subject.subList(1, 2);

        // test sublist
        assertThat(sublist).hasSize(1).containsExactly("other");
        sublist.clear();
        sublist.addLazy(() -> "new");

        assertThat(sublist).hasSize(1);
        assertThat(subject).hasSize(2);
        assertThat(subject.streamLazy().map(Lazy::isAvailable)).containsExactly(false, false);

        assertThat(sublist).containsExactly("new");
        assertThat(subject).containsExactly("test", "new");
    }

    @Test
    void testEquals() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "test");
        subject.addLazy(() -> "other");

        assertThat(subject).isEqualTo(subject)
                .isNotEqualTo(null)
                .isNotEqualTo(new Object())
                .isNotEqualTo(new LazyList<>(singletonList("other")))
                .isNotEqualTo(new LazyList<>(singletonList("test")))
                .isNotEqualTo(new LazyList<>(Arrays.asList("other", "test")))
                .isEqualTo(new LazyList<>(Arrays.asList("test", "other")))
                .isEqualTo(Arrays.asList("test", "other"))
                .isEqualTo(new ArrayList<>(Arrays.asList("test", "other")))
                .isEqualTo(new LinkedList<>(Arrays.asList("test", "other")));
    }

    @Test
    void testHashCode() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "test");
        subject.addLazy(() -> "other");

        assertThat(subject).hasSameHashCodeAs(subject)
                .hasSameHashCodeAs(new LazyList<>(Arrays.asList("test", "other")))
                .hasSameHashCodeAs(Arrays.asList("test", "other"))
                .hasSameHashCodeAs(new ArrayList<>(Arrays.asList("test", "other")))
                .hasSameHashCodeAs(new LinkedList<>(Arrays.asList("test", "other")));
    }

    @Test
    void toString_unresolved() {
        LazyList<String> subject = new LazyList<>();
        subject.addLazy(() -> "test");
        subject.addLazy(() -> "other");

        assertThat(subject).hasToString("[Lazy.unresolved, Lazy.unresolved]");
    }

    @Test
    void toString_resolved() {
        LazyList<String> subject = new LazyList<>();
        subject.add("test");
        subject.add("other");

        assertThat(subject).hasToString("[Lazy[test], Lazy[other]]");
    }
}
