[//]: # (title: Kotlin/Native FAQ)

## How do I run my program?

Define a top-level function `fun main(args: Array<String>)` or just  `fun main()` if you are not interested
in passed arguments, please ensure it's not in a package.
Also, compiler switch `-entry` could be used to make any function taking `Array<String>` or no arguments
and return `Unit` as an entry point.

## What is Kotlin/Native memory management model?

Kotlin/Native uses an automated memory management scheme that is similar to what Java or Swift provide.

[Learn about the Kotlin/Native memory manager](native-memory-manager.md)

## How do I create a shared library?

Use the `-produce dynamic` compiler option or `binaries.sharedLib()` in your Gradle build file:

```kotlin
kotlin {
    iosArm64("mylib") {
        binaries.sharedLib()
    }
}
```

It produces a platform-specific shared object (`.so` on Linux, `.dylib` on macOS, and `.dll` on Windows targets) and a
C language header, allowing the use of all public APIs available in your Kotlin/Native program from C/C++ code.

[Complete the Kotlin/Native as a dynamic library tutorial](native-dynamic-libraries.md)

## How do I create a static library or an object file?

Use the `-produce static` compiler option or `binaries.staticLib()` in your Gradle build file:

```kotlin
kotlin {
    iosArm64("mylib") {
        binaries.staticLib()
    }
}
```

It produces a platform-specific static object (`.a` library format) and a C language header, allowing you to
use all the public APIs available in your Kotlin/Native program from C/C++ code.

## How do I run Kotlin/Native behind a corporate proxy?

As Kotlin/Native needs to download a platform specific toolchain, you need to specify
`-Dhttp.proxyHost=xxx -Dhttp.proxyPort=xxx` as the compiler's or `gradlew` arguments,
or set it via the `JAVA_OPTS` environment variable.

## How do I specify a custom Objective-C prefix/name for my Kotlin framework?

Use the `-module-name` compiler option or matching Gradle DSL statement.

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
kotlin {
    iosArm64("myapp") {
        binaries.framework {
            freeCompilerArgs += listOf("-module-name", "TheName")
        }
    }
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
kotlin {
    iosArm64("myapp") {
        binaries.framework {
            freeCompilerArgs += ["-module-name", "TheName"]
        }
    }
}
```

</tab>
</tabs>

## How do I rename the iOS framework?

The default name is for an iOS framework  is `<project name>.framework`.
To set a custom name, use the `baseName` option. This will also set the module name.

```kotlin
kotlin {
    iosArm64("myapp") {
       binaries {
          framework {
              baseName = "TheName"
          }
       }
    }
}
```

## How do I enable bitcode for my Kotlin framework?

Bitcode embedding was deprecated in Xcode 14 and removed in Xcode 15 for all Apple targets.
The Kotlin/Native compiler does not support bitcode embedding since Kotlin 2.0.20.

If you're using earlier versions of Xcode but want to upgrade to Kotlin 2.0.20 or later versions, disable bitcode
embedding in your Xcode projects.

## How do I reference objects safely from different coroutines?

To safely access or update an object across multiple coroutines in Kotlin/Native, consider using concurrency-safe
constructs, such as `@Volatile` and `AtomicReference`.

Use [`@Volatile`](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.concurrent/-volatile/) to annotate a `var` property.
This makes all reads and writes to the property's backing field atomic. In addition, writes become immediately visible
to other threads. When another thread accesses this property, it observes not only the updated value but also the changes
that happened before the update.

Alternatively, use [AtomicReference](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.concurrent.atomics/-atomic-reference/),
which supports atomic reads and updates. On Kotlin/Native, it wraps a volatile variable and performs atomic operations.
Kotlin also provides a set of types for atomic operations tailored to specific data types. You can use `AtomicInt`,
`AtomicLong`, `AtomicBoolean`, `AtomicArray`, as well as `AtomicIntArray` and `AtomicLongArray`.

For more information about access to shared mutable state, see the [Coroutines documentation](shared-mutable-state-and-concurrency.md).

## How can I compile my project with unreleased versions of Kotlin/Native?

First, please consider trying [preview versions](eap.md).

In case you need an even more recent development version, you can build Kotlin/Native from source code:
clone [Kotlin repository](https://github.com/JetBrains/kotlin) and follow [these steps](https://github.com/JetBrains/kotlin/blob/master/kotlin-native/README.md#building-from-source).
