[![CI build][ci-img]][ci]
[![Coverage Status][coveralls-img]][coveralls]
[![Maven Version][maven-img]][maven]
[![JavaDoc][javadoc-img]][javadoc]

# Lazy4J

A generic `Lazy` class in java

## What is it?

A small container class that takes a supplier function and evaluates it at most once,
but not before the result is needed.  
`Lazy` wraps the `Supplier` functional interface.

## Why?

This is often recurring functionality that should have been provided out of the box by the JDK
when lambda's were introduced in Java 8.

Fortunately, it's not very difficult to create, so that's what we did.

## Example

A small example of how this class can be used:

```java
public class Example {
    private final Lazy<Expensive> expensive = Lazy.lazy(Expensive::create);
}
```

This declares a lazy `expensive` variable without creating an expensive object yet.  
Only when `get()` is called for the first time, the `Expensive.create()` method is called.  
All subsequent invocations will return _the same_ instance of `Expensive`.

Lazy provides `isAvailable`, `map`, `flatMap`, `ifAvailable` functions.  
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
  [lazy-javadoc-page]: <https://javadoc.io/page/nl.talsmasoftware/lazy4j/latest/nl/talsmasoftware/lazy4j/Lazy.html>
  