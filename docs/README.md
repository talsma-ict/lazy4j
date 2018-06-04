[![Build Status][ci-img]][ci]
[![Coverage Status][coveralls-img]][coveralls]
[![Released Version][maven-img]][maven]

# Lazy4J

A generic `Lazy` class in java

## What is it?

A small container class that takes a supplier function and evaluates it at most once,
but not before the result is needed.  
`Lazy` implements the `Supplier` functional interface.

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

This declares a lazy `expensive` variable.  
Initially, no expensive object is created yet: 
Only when `expensive.get()` is called for the first time,
the `Expensive.create()` method is called.
All subsequent invocations will just return _the same_ instance of `Expensive`.

Furthermore, a testing method `isAvailable` tells whether the lazy object 
was already resolved, along with a method `ifAvailable` that takes a consumer and
provides it with the value _only if_ it is already available,
_without_ eagerly fetching the value.

Lazy also support `map` and `flatMap` functions.

## License

[Apache 2.0 license](../LICENSE)

## Getting the library

Add the following dependency to your project
or download it [directly from github](https://github.com/talsma-ict/lazy4j/releases):

#### Maven

```xml
<dependency>
    <groupId>nl.talsmasoftware</groupId>
    <artifactId>lazy4j</artifactId>
    <version>[see maven-central badge]</version>
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


  [ci-img]: https://img.shields.io/travis/talsma-ict/lazy4j/develop.svg
  [ci]: https://travis-ci.org/talsma-ict/lazy4j
  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware/lazy4j.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware%22%20AND%20a%3A%22lazy4j%22
  [coveralls-img]: https://coveralls.io/repos/github/talsma-ict/lazy4j/badge.svg
  [coveralls]: https://coveralls.io/github/talsma-ict/lazy4j
