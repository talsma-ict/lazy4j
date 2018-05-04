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
private static final Supplier<Expensive> SINGLETON = Lazy.lazy(Expensive::create);
```

This will declare a `SINGLETON` constant of an `Expensive` type.  
Initially, no expensive object is created yet.
Only when `SINGLETON.get()` is first called, the `Expensive.create()` method is invoked, 
but never more than once for each `Lazy` instance.

## License

[Apache 2.0 license](../LICENSE)

  [ci-img]: https://img.shields.io/travis/talsma-ict/lazy4j/develop.svg
  [ci]: https://travis-ci.org/talsma-ict/lazy4j
  [maven-img]: https://img.shields.io/maven-central/v/nl.talsmasoftware/lazy4j.svg
  [maven]: http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22nl.talsmasoftware%22%20AND%20a%3A%22lazy4j%22
  [coveralls-img]: https://coveralls.io/repos/github/talsma-ict/lazy4j/badge.svg
  [coveralls]: https://coveralls.io/github/talsma-ict/lazy4j
