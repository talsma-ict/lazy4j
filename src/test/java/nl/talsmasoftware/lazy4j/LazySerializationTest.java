/*
 * Copyright 2018-2026 Talsma ICT
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.RandomAccess;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LazySerializationTest {

    @Test
    @DisplayName("Lazy: Serialization forces eager evaluation.")
    void serializationForcesEagerEvaluation() {
        // given
        Lazy<String> subject = Lazy.of(() -> "The quick brown fox jumps over the lazy dog.");
        assertThat(subject.isAvailable()).isFalse();

        // when
        assertDoesNotThrow(() -> serialize(subject));

        // then
        assertThat(subject.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Lazy: Deserialized lazy value is actually eagerly evaluated.")
    void deserializedLazyValueIsEagerlyEvaluated() throws IOException {
        // given
        byte[] serializedSubject = serialize(Lazy.of(() -> "The quick brown fox jumps over the lazy dog."));

        // when
        Lazy<String> subject = deserialize(serializedSubject);

        // then
        assertThat(subject.isAvailable()).isTrue();
        assertThat(subject.getIfAvailable()).contains("The quick brown fox jumps over the lazy dog.");
        assertThat(subject.get()).isEqualTo("The quick brown fox jumps over the lazy dog.");
    }

    @Test
    @DisplayName("Lazy: Non-serializable lazy values throw NotSerializableException.")
    void serializeLazyReferenceOfNonSerializableObject() {
        // given
        Lazy<Object> subject = Lazy.of(Object::new);

        // when
        assertThatThrownBy(() -> serialize(subject))
                // then
                .isInstanceOf(NotSerializableException.class);
    }

    @Test
    @DisplayName("LazyList: All values get eagerly evaluated upon serialization.")
    void lazyListGetsEagerlyEvaluatedUponSerialization() throws IOException {
        // given
        LazyList<String> subject = LazyList.of(() -> "The quick brown fox jumps over the lazy dog.");

        // when
        LazyList<String> result = deserialize(serialize(subject));

        // then
        assertThat(result).isNotNull().isInstanceOf(RandomAccess.class);
        assertThat(result.streamAvailable().collect(Collectors.toList())).hasSize(1);
    }
    
    @Test
    @DisplayName("LazyValueMap: All values get eagerly evaluated upon serialization.")
    void lazyValueMapGetsEagerlyEvaluatedUponSerialization() throws IOException {
        // given
        LazyValueMap<String, String> subject = new LazyValueMap<>();
        subject.putLazy("key", () -> "The quick brown fox jumps over the lazy dog.");
        assertThat(subject.getLazy("key").isAvailable()).isFalse();

        // when
        LazyValueMap<String, String> result = deserialize(serialize(subject));

        // then
        assertThat(result.getLazy("key").isAvailable()).isTrue();
        assertThat(result).containsEntry("key", "The quick brown fox jumps over the lazy dog.");
    }

    static byte[] serialize(Object object) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            oos.flush();
            baos.flush();
            return baos.toByteArray();
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(byte[] bytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (T) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Class to deserialize not found: " + e.getMessage(), e);
        }
    }
}
