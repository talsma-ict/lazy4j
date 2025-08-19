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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LazyIteratorTest {

    @Test
    void nextLazy() {
        LazyList<String> lazyList = new LazyList<>();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");

        LazyIterator<String> subject = lazyList.iterator();

        assertThat(subject.hasNext()).isTrue();
        Lazy<String> result = subject.nextLazy();
        assertThat(result.isAvailable()).isFalse();
        assertThat(result.get()).isEqualTo("one");
        assertThat(result.isAvailable()).isTrue();

        assertThat(subject.hasNext()).isTrue();
        result = subject.nextLazy();
        assertThat(result.isAvailable()).isTrue();
        assertThat(result.get()).isEqualTo("two");

        assertThat(subject.hasNext()).isTrue();
        result = subject.nextLazy();
        assertThat(result.isAvailable()).isFalse();
        assertThat(result.get()).isEqualTo("three");
        assertThat(result.isAvailable()).isTrue();
    }

    @Test
    void forEachRemainingAvailable() {
        LazyList<String> lazyList = new LazyList<>();
        lazyList.addLazy(() -> "one");
        lazyList.add("two");
        lazyList.addLazy(() -> "three");
        lazyList.add("four");
        List<String> results = new ArrayList<>();

        LazyIterator<String> subject = lazyList.iterator();

        subject.forEachRemainingAvailable(results::add);
        assertThat(results).containsExactly("two", "four");
        assertThat(subject.hasNext()).isFalse();
    }
}
