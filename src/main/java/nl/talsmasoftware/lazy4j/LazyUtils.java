package nl.talsmasoftware.lazy4j;

final class LazyUtils {
    private LazyUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    static <V> V getNullSafe(Lazy<V> lazy) {
        return lazy == null ? null : lazy.get();
    }

    static <T> T getIfAvailableElseNull(Lazy<T> lazy) {
        return lazy == null || !lazy.isAvailable() ? null : lazy.get();
    }
}
