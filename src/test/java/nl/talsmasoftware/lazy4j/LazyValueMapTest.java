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
    void put_availableExistingValue() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.put("key", "old");

        String result = subject.put("key", "new");
        assertThat(result).isEqualTo("old");
        assertThat(subject.getIfAvailable("key")).contains("new");
    }

    @Test
    void putLazy_emtpyMap() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        Lazy<String> lazy = Lazy.of(() -> "test");

        Lazy<String> result = subject.putLazy("key", lazy);
        assertThat(result).isNull();
        assertThat(lazy.isAvailable()).isFalse();
        assertThat(subject.getIfAvailable("key")).isEmpty();
        assertThat(subject.get("key")).isEqualTo("test");
    }

    @Test
    void putLazy_existingValueNotYetAvailable() {
        Lazy<String> previousLazy = Lazy.of(() -> "old");
        Lazy<String> newLazy = Lazy.of(() -> "new");
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", previousLazy);

        Lazy<String> result = subject.putLazy("key", newLazy);
        assertThat(previousLazy.isAvailable()).isFalse();
        assertThat(newLazy.isAvailable()).isFalse();
        assertThat(result.isAvailable()).isFalse();

        assertThat(result.get()).isEqualTo("old");
        assertThat(previousLazy.isAvailable()).isTrue();
        assertThat(newLazy.isAvailable()).isFalse();
        assertThat(result.isAvailable()).isTrue();

        assertThat(subject.get("key")).isEqualTo("new");
        assertThat(newLazy.isAvailable()).isTrue();
    }

    @Test
    void putLazy_existingValueAlreadyAvailable() {
        Lazy<String> newLazy = Lazy.of(() -> "new");
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.put("key", "old");

        Lazy<String> result = subject.putLazy("key", newLazy);
        assertThat(newLazy.isAvailable()).isFalse();
        assertThat(result.isAvailable()).isTrue();

        assertThat(subject.get("key")).isEqualTo("new");
        assertThat(newLazy.isAvailable()).isTrue();
    }

    @Test
    void containsValue_emptyMap() {
        assertThat(new LazyValueMap<>().containsValue("test")).isFalse();
    }

    @Test
    void containsValue() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key1", () -> "lazy1");
        subject.put("eager", "eager");
        subject.putLazy("key2", () -> "lazy2");

        // First pass; check available values, leaving lazy values alone.
        assertThat(subject.containsValue("eager")).isTrue();

        assertThat(subject.getLazy("key1").isAvailable()).isFalse();
        assertThat(subject.getLazy("key2").isAvailable()).isFalse();

        // Second pass; check lazy values, stopping when the first entry is found.
        assertThat(subject.containsValue("lazy1")).isTrue();
        assertThat(subject.getLazy("key1").isAvailable()).isTrue();
        assertThat(subject.getLazy("key2").isAvailable()).isFalse();

        // Searching for non-existing value requires eager evaluation of all values.
        assertThat(subject.containsValue("lazy3")).isFalse();
        assertThat(subject.getLazy("key2").isAvailable()).isTrue();
    }

    @Test
    void containsValue_firstPassMemoryIsLimited() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        for (int i = 1; i < 100000; i++) {
            subject.putLazy("key" + i, () -> "lazy value");
        }
        subject.put("eager", "eager value");

        assertThat(subject.containsValue("eager value")).isTrue();
        for (int i = 1; i < 100000; i++) {
            assertThat(subject.getLazy("key" + i).isAvailable()).isFalse();
        }

        assertThat(subject.containsValue("does not occur")).isFalse();
        subject.lazyValues().stream().forEach(value -> assertThat(value.isAvailable()).isTrue());
    }
}
