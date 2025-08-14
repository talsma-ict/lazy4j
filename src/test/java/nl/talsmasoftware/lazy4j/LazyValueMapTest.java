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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class LazyValueMapTest {
    @Test
    void copyConstructor_eager() {
        Map<String, String> toCopy = new LinkedHashMap<>();
        toCopy.put("key1", "value1");
        toCopy.put("key2", "value2");

        LazyValueMap<String, String> subject = new LazyValueMap<>(toCopy);

        for (Lazy<String> value : subject.lazyValues()) {
            assertThat(value.isAvailable()).isTrue(); // All values were eagerly evaluated.
        }
    }

    @Test
    void copyConstructor_lazy() {
        LazyValueMap<String, String> toCopy = new LazyValueMap<>();
        toCopy.putLazy("key1", () -> "value1");
        toCopy.putLazy("key2", () -> "value2");

        LazyValueMap<String, String> subject = new LazyValueMap<>(toCopy);

        for (Lazy<String> value : subject.lazyValues()) {
            assertThat(value.isAvailable()).isFalse(); // No lazy values were eagerly evaluated.
        }
        for (Lazy<String> value : toCopy.lazyValues()) {
            assertThat(value.isAvailable()).isFalse(); // Neither in the source map.
        }

        assertThat(subject.get("key2")).isEqualTo("value2");
        assertThat(toCopy.getLazy("key2").isAvailable()).isTrue();
    }

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
    void entrySet_accessingKeysDoesNotTriggerEvaluation() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", lazy);

        assertThat(subject.entrySet()).hasSize(1);
        for (Map.Entry<String, String> entry : subject.entrySet()) {
            assertThat(entry.getKey()).isEqualTo("key");
        }

        assertThat(lazy.isAvailable()).isFalse();
    }

    @Test
    void entrySet_accessingValuesTriggersEagerEvaluation() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", lazy);

        assertThat(subject.entrySet()).hasSize(1);
        for (Map.Entry<String, String> entry : subject.entrySet()) {
            assertThat(entry.getValue()).isEqualTo("test");
        }

        assertThat(lazy.isAvailable()).isTrue();
    }

    @Test
    void entrySet_setValue() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", lazy);

        assertThat(subject.entrySet()).hasSize(1);
        for (Map.Entry<String, String> entry : subject.entrySet()) {
            assertThat(entry.setValue("other")).isNull(); // lazy was not yet available.
        }
        assertThat(subject.getLazy("key").isAvailable()).isTrue();
        assertThat(subject.get("key")).isEqualTo("other");

        assertThat(subject.entrySet()).hasSize(1);
        for (Map.Entry<String, String> entry : subject.entrySet()) {
            assertThat(entry.setValue("yet another")).isEqualTo("other");
        }
        assertThat(subject.getLazy("key").isAvailable()).isTrue();
        assertThat(subject.get("key")).isEqualTo("yet another");

        assertThat(lazy.isAvailable()).isFalse(); // No eager evaluation of lazy value.
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
        assertThat(subject.getLazy("key").getIfAvailable()).contains("test");
    }

    @Test
    void put_existingValueNotYetEvaluated() {
        Lazy<String> previousLazyValue = Lazy.of(() -> "old");
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", previousLazyValue);

        String result = subject.put("key", "new");
        assertThat(result).isNull();
        assertThat(previousLazyValue.isAvailable()).isFalse();
        assertThat(subject.getLazy("key").getIfAvailable()).contains("new");
    }

    @Test
    void put_availableExistingValue() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.put("key", "old");

        String result = subject.put("key", "new");
        assertThat(result).isEqualTo("old");
        assertThat(subject.getLazy("key").getIfAvailable()).contains("new");
    }

    @Test
    void putLazy_emtpyMap() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        Lazy<String> lazy = Lazy.of(() -> "test");

        Lazy<String> result = subject.putLazy("key", lazy);
        assertThat(result).isNull();
        assertThat(lazy.isAvailable()).isFalse();
        assertThat(subject.getLazy("key").getIfAvailable()).isEmpty();
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

    @Test
    void size_mustMatchBackingMap() {
        Map<String, Lazy<String>> backingMap = new java.util.LinkedHashMap<>();
        LazyValueMap<String, String> subject = new LazyValueMap<>(() -> backingMap);

        assertThat(subject).isEmpty();
        backingMap.put("key1", Lazy.of(() -> "value1"));
        assertThat(subject).hasSize(1);
        backingMap.put("key2", Lazy.of(() -> "value2"));
        assertThat(subject).hasSize(2);
        backingMap.remove("key1");
        assertThat(subject).hasSize(1);
        subject.clear();
        assertThat(subject).isEmpty();
        assertThat(backingMap).isEmpty();
    }

    @Test
    void containsKey() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key1", () -> "lazy1");

        assertThat(subject.containsKey("key1")).isTrue();
        assertThat(subject.containsKey("key2")).isFalse();
        assertThat(subject.getLazy("key1").isAvailable()).isFalse();
    }

    @Test
    void remove() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        Lazy<String> lazy = Lazy.of(() -> "test");
        subject.putLazy("lazy", lazy);
        subject.put("eager", "eager");

        assertThat(subject.remove("missing")).isNull();
        assertThat(subject.remove("eager")).isEqualTo("eager");
        assertThat(subject.remove("lazy")).isNull();
        assertThat(lazy.isAvailable()).isFalse();
    }

    @Test
    void keySet() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        assertThat(subject.keySet()).isEmpty();
        subject.put("eager", "eager");
        assertThat(subject.keySet()).containsExactly("eager");
        subject.putLazy("lazy", () -> "lazy");
        assertThat(subject.keySet()).containsExactly("eager", "lazy");
        assertThat(subject.getLazy("lazy").isAvailable()).isFalse();
    }

    @Test
    void computeIfAbsent() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        assertThat(subject.computeIfAbsent("key", k -> "value")).isEqualTo("value");
        assertThat(subject.get("key")).isEqualTo("value");

        assertThat(subject.computeIfAbsent("key", k -> "other")).isEqualTo("value");
        assertThat(subject.get("key")).isEqualTo("value");
    }

    @Test
    void lazyComputeIfAbsent() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        AtomicBoolean called = new AtomicBoolean(false);

        subject.lazyComputeIfAbsent("key", k -> {
            called.set(true);
            return "value";
        });
        assertThat(subject.containsKey("key")).isTrue();
        assertThat(called.get()).isFalse();

        assertThat(subject.get("key")).isEqualTo("value");
        assertThat(called.get()).isTrue();
    }

    @Test
    void putIfAbsent() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        assertThat(subject.putIfAbsent("key", "value")).isNull();
        assertThat(subject.get("key")).isEqualTo("value");

        assertThat(subject.putIfAbsent("key", "other")).isEqualTo("value");
        assertThat(subject.get("key")).isEqualTo("value");
    }

    @Test
    void putLazyIfAbsent() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        Lazy<String> lazy = Lazy.of(() -> "value");

        assertThat(subject.lazyPutIfAbsent("key", lazy)).isNull();
        assertThat(lazy.isAvailable()).isFalse();

        assertThat(subject.lazyPutIfAbsent("key", () -> "other").isAvailable()).isFalse();
        assertThat(lazy.isAvailable()).isFalse();

        assertThat(subject.get("key")).isEqualTo("value");
        assertThat(lazy.isAvailable()).isTrue();
    }

    @Test
    void replace() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        assertThat(subject.replace("newKey", "other")).isNull();
        assertThat(subject.containsKey("newKey")).isFalse();

        subject.put("key", "value");
        assertThat(subject.replace("key", "other")).isEqualTo("value");

        Lazy<String> lazy = Lazy.of(() -> "lazy value");
        subject.putLazy("key", lazy);
        assertThat(subject.replace("key", "other")).isNull(); // lazy value not eagerly evaluated.
        assertThat(subject.get("key")).isEqualTo("other");
        assertThat(lazy.isAvailable()).isFalse();
    }

    @Test
    void lazyReplace() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        assertThat(subject.lazyReplace("newKey", () -> "other")).isNull();
        assertThat(subject.containsKey("newKey")).isFalse();

        subject.putLazy("key", () -> "lazy value");
        Lazy<String> result = subject.lazyReplace("key", () -> "other value");
        assertThat(result.isAvailable()).isFalse();
        assertThat(subject.getLazy("key").isAvailable()).isFalse();

        assertThat(result.get()).isEqualTo("lazy value");
        assertThat(subject.get("key")).isEqualTo("other value");
    }

    @Test
    void computeIfPresent() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        AtomicBoolean called = new AtomicBoolean(false);

        // not present
        String result = subject.computeIfPresent("newKey", (k, v) -> {
            called.set(true);
            return "other";
        });
        assertThat(result).isNull();
        assertThat(called.get()).isFalse();
        assertThat(subject.containsKey("newKey")).isFalse();

        // present lazy value
        subject.putLazy("key", () -> "value");
        result = subject.computeIfPresent("key", (k, v) -> {
            called.set(true);
            return v + "+other";
        });
        assertThat(result).isEqualTo("value+other");
        assertThat(called.get()).isTrue();
        assertThat(subject.containsKey("key")).isTrue();
        assertThat(subject.getLazy("key").isAvailable()).isTrue();
        assertThat(subject.get("key")).isEqualTo("value+other");
    }

    @Test
    void lazyComputeIfPresent() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        AtomicBoolean called = new AtomicBoolean(false);

        // not present
        Lazy<String> result = subject.lazyComputeIfPresent("newKey", (k, v) -> {
            called.set(true);
            return v + "other";
        });
        assertThat(result).isNull();
        assertThat(called.get()).isFalse();
        assertThat(subject.containsKey("newKey")).isFalse();

        // lazy value
        subject.putLazy("key", () -> "value");
        result = subject.lazyComputeIfPresent("key", (k, v) -> {
            called.set(true);
            return v + "+other";
        });
        assertThat(result.isAvailable()).isFalse();
        assertThat(called.get()).isFalse();
        assertThat(subject.containsKey("key")).isTrue();
        assertThat(subject.getLazy("key").isAvailable()).isFalse();

        assertThat(subject.get("key")).isEqualTo("value+other");
        assertThat(result.isAvailable()).isTrue();
        assertThat(result.get()).isEqualTo("value+other");
    }

    @Test
    void compute() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        AtomicBoolean called = new AtomicBoolean(false);

        // No existing value
        String result = subject.compute("newKey", (k, v) -> {
            called.set(true);
            return v + "+other";
        });
        assertThat(result).isEqualTo("null+other");
        assertThat(called.get()).isTrue();
        assertThat(subject.containsKey("newKey")).isTrue();
        assertThat(subject.getLazy("newKey").isAvailable()).isTrue();
        assertThat(subject.get("newKey")).isEqualTo("null+other");

        // Existing value
        subject.putLazy("key", () -> "value");
        called.set(false);
        result = subject.compute("key", (k, v) -> {
            called.set(true);
            return v + "+other";
        });
        assertThat(result).isEqualTo("value+other");
        assertThat(called.get()).isTrue();
        assertThat(subject.containsKey("key")).isTrue();
        assertThat(subject.getLazy("key").isAvailable()).isTrue();
        assertThat(subject.get("key")).isEqualTo("value+other");
    }

    @Test
    void lazyCompute() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        AtomicBoolean called = new AtomicBoolean(false);

        // No existing value
        Lazy<String> result = subject.lazyCompute("newKey", (k, v) -> {
            called.set(true);
            return v + "+other";
        });
        assertThat(result.isAvailable()).isFalse();
        assertThat(called.get()).isFalse();
        assertThat(subject.containsKey("newKey")).isTrue();
        assertThat(subject.getLazy("newKey").isAvailable()).isFalse();
        assertThat(subject.get("newKey")).isEqualTo("null+other");

        // Existing value
        subject.putLazy("key", () -> "value");
        called.set(false);
        result = subject.lazyCompute("key", (k, v) -> {
            called.set(true);
            return v + "+other";
        });
        assertThat(result.isAvailable()).isFalse();
        assertThat(called.get()).isFalse();
        assertThat(subject.containsKey("key")).isTrue();
        assertThat(subject.getLazy("key").isAvailable()).isFalse();
        assertThat(subject.get("key")).isEqualTo("value+other");
    }

    @Test
    void merge() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        AtomicBoolean called = new AtomicBoolean(false);

        // No existing value
        String result = subject.merge("newKey", "newValue", (v1, v2) -> {
            called.set(true);
            return v1 + v2;
        });
        assertThat(result).isEqualTo("newValue");
        assertThat(called.get()).isFalse();
        assertThat(subject.containsKey("newKey")).isTrue();
        assertThat(subject.getLazy("newKey").isAvailable()).isTrue();
        assertThat(subject.get("newKey")).isEqualTo("newValue");

        // Existing lazy value
        subject.putLazy("key", () -> "oldValue");
        called.set(false);
        result = subject.merge("key", "newValue", (v1, v2) -> {
            called.set(true);
            return v1 + v2;
        });
        assertThat(result).isEqualTo("oldValuenewValue");
        assertThat(called.get()).isTrue();
        assertThat(subject.containsKey("key")).isTrue();
        assertThat(subject.getLazy("key").isAvailable()).isTrue();
        assertThat(subject.get("key")).isEqualTo("oldValuenewValue");
    }

    @Test
    void lazyMerge_noExistingValue() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        AtomicBoolean called = new AtomicBoolean(false);

        Lazy<String> result = subject.lazyMerge("newKey", () -> "newValue", (v1, v2) -> {
            called.set(true);
            return v1 + v2;
        });
        assertThat(subject.containsKey("newKey")).isTrue();
        assertThat(called.get()).isFalse();
        assertThat(result.isAvailable()).isFalse();
        assertThat(subject.getLazy("newKey").isAvailable()).isFalse();

        assertThat(subject.get("newKey")).isEqualTo("newValue");
        assertThat(result.isAvailable()).isTrue();
        assertThat(called.get()).isFalse();
    }

    @Test
    void lazyMergeValue() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        AtomicBoolean called = new AtomicBoolean(false);
        Lazy<String> previousLazy = Lazy.of(() -> "oldValue");
        Lazy<String> newLazy = Lazy.of(() -> "newValue");
        subject.putLazy("key", previousLazy);

        Lazy<String> result = subject.lazyMerge("key", newLazy, (v1, v2) -> {
            called.set(true);
            return v1 + v2;
        });
        // Merge is not performed until the lazy value is accessed.
        assertThat(subject.containsKey("key")).isTrue();
        assertThat(called.get()).isFalse();
        assertThat(result.isAvailable()).isFalse();
        assertThat(newLazy.isAvailable()).isFalse();
        assertThat(previousLazy.isAvailable()).isFalse();

        // Accessing the result triggers eager merge.
        assertThat(result.get()).isEqualTo("oldValuenewValue");
        assertThat(called.get()).isTrue();
        assertThat(subject.getLazy("key").isAvailable()).isTrue();
        assertThat(newLazy.isAvailable()).isTrue();
        assertThat(previousLazy.isAvailable()).isTrue();
        assertThat(subject.get("key")).isEqualTo("oldValuenewValue");
    }

    @Test
    void equals_otherMap() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", () -> "value");

        Map<String, String> otherMap = new TreeMap<>();
        otherMap.put("key", "value");

        assertThat(subject).isEqualTo(otherMap);
        assertThat(otherMap).isEqualTo(subject);
        assertThat(subject.getLazy("key").isAvailable()).isTrue();
    }

    @Test
    void hashCode_sameAsOtherMaps() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", () -> "value");
        subject.putLazy(null, () -> null);

        Map<String, String> otherMap = new HashMap<>();
        otherMap.put("key", "value");
        otherMap.put(null, null);

        assertThat(subject).hasSameHashCodeAs(otherMap);
        assertThat(subject.getLazy("key").isAvailable()).isTrue();
    }

    @Test
    void toString_lazyValues() {
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", () -> "value");

        assertThat(subject.toString()).isEqualTo("{key=Lazy.unresolved}");
        assertThat(subject.getLazy("key").isAvailable()).isFalse();

        subject.get("key");
        assertThat(subject.toString()).isEqualTo("{key=Lazy[value]}");
        assertThat(subject.getLazy("key").isAvailable()).isTrue();
    }

    @Test
    void entryHashCode() {
        LazyValueMap<String, String> lazyMap = new LazyValueMap<>();
        lazyMap.putLazy("key", () -> "value");

        Map.Entry<String, String> subject = lazyMap.entrySet().iterator().next();

        assertThat(subject.hashCode()).isEqualTo(Collections.singletonMap("key", "value").hashCode());
    }

    @Test
    void entryEquals() {
        LazyValueMap<String, String> lazyMap = new LazyValueMap<>();
        lazyMap.putLazy("key", () -> "value");

        Map.Entry<String, String> subject = lazyMap.entrySet().iterator().next();
        Map.Entry<String, String> other = Collections.singletonMap("key", "value").entrySet().iterator().next();

        assertThat(subject).isEqualTo(subject);
        assertThat(subject).isEqualTo(other);
        assertThat(other).isEqualTo(subject);
        assertThat(subject).isNotEqualTo(null);
        assertThat(subject).isNotEqualTo("key=value");
        assertThat(subject).isNotEqualTo(Collections.singletonMap("other", "value").entrySet().iterator().next());
        assertThat(subject).isNotEqualTo(Collections.singletonMap("key", "other").entrySet().iterator().next());
    }

    @Test
    void entryToString() {
        LazyValueMap<String, String> lazyMap = new LazyValueMap<>();
        lazyMap.putLazy("key", () -> "value");

        // toString unresolved.
        Map.Entry<String, String> subject = lazyMap.entrySet().iterator().next();
        assertThat(subject).hasToString("key=Lazy.unresolved");

        // toString resolved.
        subject.getValue();
        assertThat(subject).hasToString("key=Lazy[value]");
    }
}
