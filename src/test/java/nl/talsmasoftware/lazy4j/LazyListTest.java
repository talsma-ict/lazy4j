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

import java.util.Arrays;

import static java.util.Collections.singletonList;
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
}
