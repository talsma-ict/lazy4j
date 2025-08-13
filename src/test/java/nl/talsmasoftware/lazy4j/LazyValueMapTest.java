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

import static org.assertj.core.api.Assertions.assertThat;

class LazyValueMapTest {
    @Test
    void entrySet_emptyMap() {
        assertThat(new LazyValueMap<>().entrySet()).isEmpty();
    }

    @Test
    void entrySet_clear() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", lazy);

        assertThat(subject.entrySet()).hasSize(1);
        subject.entrySet().clear();

        assertThat(subject.entrySet()).isEmpty();
        assertThat(subject.isEmpty());
        assertThat(lazy.isAvailable()).isFalse();
    }

    @Test
    void getIfAvailable_emptyMap() {
        assertThat(new LazyValueMap<>().getIfAvailable("key")).isEmpty();
    }

    @Test
    void getIfAvailable_notYetEvaluated() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", lazy);

        assertThat(subject.getIfAvailable("key")).isEmpty();
        assertThat(lazy.isAvailable()).isFalse();
    }

    @Test
    void getIfAvailable_evaluatedValueNull() {
        Lazy<String> lazy = Lazy.eager(null);
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", lazy);

        assertThat(subject.getIfAvailable("key")).isEmpty();
        assertThat(lazy.isAvailable()).isTrue();
    }

    @Test
    void getIfAvailable() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", lazy);

        assertThat(subject.getIfAvailable("key")).isEmpty();
        assertThat(subject.get("key")).isEqualTo("test");
        assertThat(subject.getIfAvailable("key")).contains("test");
        assertThat(lazy.isAvailable()).isTrue();
    }

    @Test
    void get_emptyMap() {
        assertThat(new LazyValueMap<>().get("key")).isNull();
    }

    @Test
    void get_notYetEvaluated() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", lazy);

        assertThat(subject.get("key")).isEqualTo("test");
        assertThat(lazy.isAvailable()).isTrue();
    }

    @Test
    void put_emptyMap() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();

        String result = subject.put("key", "test");
        assertThat(result).isNull();
        assertThat(subject.getIfAvailable("key")).contains("test");
    }

    @Test
    void put_existingValueNotYetEvaluated() {
        Lazy<String> previousLazyValue = Lazy.of(() -> "old");
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", previousLazyValue);

        String result = subject.put("key", "new");
        assertThat(result).isNull();
        assertThat(previousLazyValue.isAvailable()).isFalse();
        assertThat(subject.getIfAvailable("key")).contains("new");
    }

    @Test
    void put_existingValueAlreadyEvaluated() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.put("key", "old");

        String result = subject.put("key", "new");
        assertThat(result).isEqualTo("old");
        assertThat(subject.getIfAvailable("key")).contains("new");
    }

}
