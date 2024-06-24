[![CI build][ci-img]][ci]
[![Coverage Status][coveralls-img]][coveralls]
[![Maven Version][maven-img]][maven]
[![JavaDoc][javadoc-img]][javadoc]

# Lazy4J

A generic `Lazy` class in java

## What is it?

A lazy function that is evaluated only when it is first needed,
remembering the result so it does not get called again.

Technically, `Lazy` is a wrapper for standard Java `Supplier` functions.

## Why?

We feel this ought to be provided out of the box and should have been when lambda's were introduced, back in Java 8.

Fortunately, it's not very difficult to create, so that's what we did.

## Example

A small example of how this class can be used:

```java
public class Example {
    // Method reference to Expensive.create() only when needed and remember the result.
    private final Lazy<Expensive> lazyMethod = Lazy.lazy(Expensive::new);

    // Lambda called only once when needed for the first time.
    private final Lazy<Expensive> lazyLambda = Lazy.lazy(() -> new Expensive());
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


  [ci-img]: https://github.com/talsma-ict/lazy4j/actions/workflows/ci-build.yml/badge.svg
  [ci]: https://github.com/talsma-ict/lazy4j/actions/workflows/ci-build.yml
  [maven-img]: <https://img.shields.io/maven-central/v/nl.talsmasoftware/lazy4j>
  [maven]: <http://mvnrepository.com/artifact/nl.talsmasoftware/lazy4j>
  [coveralls-img]: <https://coveralls.io/repos/github/talsma-ict/lazy4j/badge.svg>
  [coveralls]: <https://coveralls.io/github/talsma-ict/lazy4j>
  [javadoc-img]: <https://www.javadoc.io/badge/nl.talsmasoftware/lazy4j.svg>
  [javadoc]: <https://www.javadoc.io/doc/nl.talsmasoftware/lazy4j>
  [lazy-javadoc-page]: <https://www.javadoc.io/static/nl.talsmasoftware/lazy4j/2.0.0/nl.talsmasoftware.lazy4j/nl/talsmasoftware/lazy4j/package-summary.html>
  
