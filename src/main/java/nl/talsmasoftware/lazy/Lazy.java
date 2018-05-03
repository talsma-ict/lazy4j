/*
 * Copyright 2018 Talsma ICT
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
package nl.talsmasoftware.lazy;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A generic `Lazy` class in java.
 *
 * @author Sjoerd Talsma
 */
public class Lazy<T> implements Supplier<T> {

    private final Supplier<T> supplier;
    private volatile boolean resolved;
    private volatile T result;
    private volatile RuntimeException exception;

    private Lazy(Supplier<T> supplier) {
        this.supplier = requireNonNull(supplier, "Lazy supplier is <null>.");
        this.resolved = false;
    }

    public static <T> Lazy<T> evaluate(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    @Override
    public T get() {
        synchronized (this) {
            if (!resolved) {
                resolved = true;
                try {
                    result = supplier.get();
                } catch (RuntimeException supplierException) {
                    exception = supplierException;
                }
            }
        }
        if (exception != null) {
            throw new LazyEvaluationException("Could not evaluate lazy value: " + exception.getMessage(), exception);
        }
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + (!resolved ? "[not yet resolved]"
                : exception == null ? "[" + result + ']'
                : "[threw exception]");
    }

}
