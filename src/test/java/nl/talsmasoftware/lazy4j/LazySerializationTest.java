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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LazySerializationTest {

    @Test
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
    void deserializedLazyValueIsEagerlyEvaluated() {
        // given
        byte[] serializedSubject = serialize(Lazy.of(() -> "The quick brown fox jumps over the lazy dog."));

        // when
        Lazy<String> subject = deserialize(serializedSubject);

        // then
        assertThat(subject.isAvailable()).isTrue();
        assertThat(subject.getIfAvailable()).contains("The quick brown fox jumps over the lazy dog.");
        assertThat(subject.get()).isEqualTo("The quick brown fox jumps over the lazy dog.");
    }

    static byte[] serialize(Serializable object) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            oos.flush();
            baos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new AssertionError("Failure serializing object: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    static <S extends Serializable> S deserialize(byte[] bytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (S) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new AssertionError("Failure deserializing object: " + e.getMessage(), e);
        }
    }
}
