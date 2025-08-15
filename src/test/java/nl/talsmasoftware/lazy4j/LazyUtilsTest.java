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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LazyUtilsTest {
    @Test
    void verifyUnsupportedConstructor() throws NoSuchMethodException {
        Constructor<LazyUtils> constructor = LazyUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .cause()
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getNullSafe_null() {
        assertThat(LazyUtils.getNullSafe((Lazy<String>) null)).isNull();
    }

    @Test
    void getNullSafe_lazy() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        assertThat(lazy.isAvailable()).isFalse();
        assertThat(LazyUtils.getNullSafe(lazy)).isEqualTo("test");
        assertThat(lazy.isAvailable()).isTrue();
    }

    @Test
    void getIfAvailableElseNull_null() {
        assertThat(LazyUtils.getIfAvailableElseNull((Lazy<String>) null)).isNull();
    }

    @Test
    void getIfAvailableElseNull_lazy() {
        Lazy<String> lazy = Lazy.of(() -> "test");
        assertThat(lazy.isAvailable()).isFalse();
        assertThat(LazyUtils.getIfAvailableElseNull(lazy)).isNull();
        assertThat(lazy.isAvailable()).isFalse();
        lazy.get();
        assertThat(LazyUtils.getIfAvailableElseNull(lazy)).isEqualTo("test");
        assertThat(lazy.isAvailable()).isTrue();
    }

    @Test
    void getIfAvailableElseNull_eager() {
        assertThat(LazyUtils.getIfAvailableElseNull(Lazy.eager("test"))).isEqualTo("test");
    }

}
