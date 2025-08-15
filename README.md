[![Maven Version][maven-img]][maven]
[![JavaDoc][javadoc-img]][javadoc]
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=talsma-ict_lazy4j&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=talsma-ict_lazy4j)
[![Coverage Status][coveralls-img]][coveralls]

# Lazy4J

- A `Lazy` class in java.
- A `LazyValueMap`.

## What is it?

### Lazy

Wrapper for a standard java `Supplier` function that only evaluates the wrapped function when it is first needed,
remembering the result so it does not need to get called again.

### LazyValueMap

Map that stores values as `Lazy` values.

## Why?

We feel this ought to be provided out of the box and should have been when lambda's were introduced, back in Java 8.

Fortunately, a `Lazy` class implementation is not very difficult to create, so that's what we did.  
The `LazyValueMap` was added later in version 2.0.2.

## Example

A small example of how the `Lazy` class can be used:

```java
public class Example {
    // Method reference to constructor new Expensive(), called only when needed and keep the result.
    private final Lazy<Expensive> lazyMethod = Lazy.of(Expensive::new);

    // Lambda called only once when needed for the first time.
    private final Lazy<Expensive> lazyLambda = Lazy.of(() -> new Expensive());
}
```

This declares a lazy variable without calling the expensive supplier yet.  
Only when `get()` is called for the first time, the `new Expensive()` constructor is called.  
All subsequent invocations will return _the same_ instance of `Expensive`.

Lazy provides the following methods:
- `isAvailable` returning whether the lazy value is already available. 
- `map` applies a function on the lazy result.
- `flatMap` applies a function that itself returns a supplier.
- `ifAvailable` runs a function only if the lazy value is already available.

Please refer to the [Lazy class documentation][lazy-javadoc-page] for full descriptions.

### LazyValueMap example

```java
    // Map that stores lazy values that only get evaluated when they are needed for the first time.
    private final LazyValueMap<String, String> lazyMap = new LazyValueMap(HashMap::new);
    private final LazyValueMap<String, String> sortedLazyMap = new LazyValueMap(TreeMap::new);

    lazyMap.putLazy("key", () -> new Expensive());
```

The expensive value is only evaluated when the key is first accessed.

The lazy map offers the following lazy variants of standard map methods:
- `getLazy`
- `putLazy`
- `lazyPutIfAbsent`
- `lazyCompute`
- `lazyComputeIfAbsent`
- `lazyComputeIfPresent`
- `lazyReplace`
- `lazyMerge`
- `lazyValues`

## Getting the class

Add the following dependency to your project
or download it [directly from github](https://github.com/talsma-ict/lazy4j/releases):

#### Maven

```xml
<dependency>
    <groupId>nl.talsmasoftware</groupId>
    <artifactId>lazy4j</artifactId>
    <version>[see maven badge]</version>
</dependency>
```

#### Gradle

```groovy
compile 'nl.talsmasoftware:lazy4j:[see maven-central badge]'
```

#### Scala

```scala
libraryDependencies += "nl.talsmasoftware" % "lazy4j" % "[see maven-central badge]"
```

## License

[Apache 2.0 license](LICENSE)


  [maven-img]: <https://img.shields.io/maven-central/v/nl.talsmasoftware/lazy4j>
  [maven]: <http://mvnrepository.com/artifact/nl.talsmasoftware/lazy4j>
  [coveralls-img]: <https://coveralls.io/repos/github/talsma-ict/lazy4j/badge.svg>
  [coveralls]: <https://coveralls.io/github/talsma-ict/lazy4j>
  [javadoc-img]: <https://www.javadoc.io/badge/nl.talsmasoftware/lazy4j.svg>
  [javadoc]: <https://www.javadoc.io/doc/nl.talsmasoftware/lazy4j>
  [lazy-javadoc-page]: <https://www.javadoc.io/static/nl.talsmasoftware/lazy4j/2.0.0/nl.talsmasoftware.lazy4j/nl/talsmasoftware/lazy4j/package-summary.html>
  
