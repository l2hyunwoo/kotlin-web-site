[//]: # (title: Test code using JUnit in JVM – tutorial)

This tutorial shows you how to write a simple unit test in a Kotlin/JVM project and run it with the Gradle build tool.

In this project, you'll use the [`kotlin.test`](https://kotlinlang.org/api/latest/kotlin.test/index.html) library and run the test using JUnit.
If you're working on a multiplatform app, see the [Kotlin Multiplatform tutorial](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-run-tests.html).

To get started, first download and install the latest version of [IntelliJ IDEA](https://www.jetbrains.com/idea/download/index.html).

## Add dependencies

1. Open a Kotlin project in IntelliJ IDEA. If you don't have a project,
   [create one](https://www.jetbrains.com/help/idea/create-your-first-kotlin-app.html#create-project).

2. Open the `build.gradle(.kts)` file and check that the `testImplementation` dependency is present.
   This dependency allows you to work with `kotlin.test` and `JUnit`:

    <tabs group="build-script">
    <tab title="Kotlin" group-key="kotlin">

   ```kotlin
   dependencies {
       // Other dependencies.
       testImplementation(kotlin("test"))
   }
   ```

    </tab>
    <tab title="Groovy" group-key="groovy">

   ```groovy
   dependencies {
       // Other dependencies.
       testImplementation 'org.jetbrains.kotlin:kotlin-test'
   }
   ```

   </tab>
   </tabs>

3. Add the `test` task to the `build.gradle(.kts)` file:

    <tabs group="build-script">
    <tab title="Kotlin" group-key="kotlin">

   ```kotlin
   tasks.test {
       useJUnitPlatform()
   }
   ```

    </tab>
    <tab title="Groovy" group-key="groovy">

   ```groovy
   test {
       useJUnitPlatform()
   }
   ```

   </tab>
   </tabs>

   > If you use the `useJUnitPlatform()`function in your build script, 
   > the `kotlin-test` library automatically includes JUnit 5 as a dependency.
   > This setup enables access to all JUnit 5 APIs, along with the `kotlin-test` API,
   > in JVM-only projects and JVM tests of Kotlin Multiplatform (KMP) projects.
   >
   {style="note"}

Here's a complete code for the `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "%kotlinVersion%"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
```
{initial-collapse-state="collapsed" collapsible="true"}

## Add the code to test it

1. Open the `Main.kt` file in `src/main/kotlin`.

   The `src` directory contains Kotlin source files and resources. 
   The `Main.kt` file contains sample code that prints `Hello, World!`.

2. Create the `Sample` class with the `sum()` function that adds two integers together:

   ```kotlin
   class Sample() {
       fun sum(a: Int, b: Int): Int {
           return a + b
       }
   }
   ```

## Create a test

1. In IntelliJ IDEA, select **Code** | **Generate** | **Test...** for the `Sample` class:

   ![Generate a test](generate-test.png)

2. Specify the name of the test class. For example, `SampleTest`:

   ![Create a test](create-test.png)

   IntelliJ IDEA creates the `SampleTest.kt` file in the `test` directory.
   This directory contains Kotlin test source files and resources.

   > You can also manually create a `*.kt` file for tests in `src/test/kotlin`.
   >
   {style="note"}

3. Add the test code for the `sum()` function in `SampleTest.kt`:

   * Define the test `testSum()` function using the [`@Test` annotation](https://kotlinlang.org/api/latest/kotlin.test/kotlin.test/-test/index.html).
   * Check that the `sum()` function returns the expected value by using the [`assertEquals()`](https://kotlinlang.org/api/latest/kotlin.test/kotlin.test/assert-equals.html) function.

   ```kotlin
   import org.example.Sample
   import org.junit.jupiter.api.Assertions.*
   import kotlin.test.Test

   class SampleTest {
       private val testSample: Sample = Sample()

       @Test
       fun testSum() {
           val expected = 42
           assertEquals(expected, testSample.sum(40, 2))
       }
   }
   ```

## Run a test

1. Run the test using the gutter icon:

   ![Run the test](run-test.png)

   > You can also run all project tests via the command-line interface using the `./gradlew check` command.
   >
   {style="note"}

2. Check the result in the **Run** tool window:

   ![Check the test result. The test passed successfully](test-successful.png)

   The test function was executed successfully.

3. Make sure that the test works correctly by changing the `expected` variable value to 43:

   ```kotlin
   @Test
   fun testSum() {
       val expected = 43
       assertEquals(expected, classForTesting.sum(40, 2))
   }
   ```

4. Run the test again and check the result:

   ![Check the test result. The test has failed](test-failed.png)

   The test execution failed.

## What's next

Once you've finished your first test, you can:

* Write more tests using other [`kotlin.test`](https://kotlinlang.org/api/latest/kotlin.test/kotlin.test/) functions.
   For example, use the [`assertNotEquals()`](https://kotlinlang.org/api/latest/kotlin.test/kotlin.test/assert-not-equals.html) function.
* Improve your test output with the [Kotlin Power-assert compiler plugin](power-assert.md).
   The plugin enriches the test output with contextual information.
* [Create your first server-side application](jvm-get-started-spring-boot.md) with Kotlin and Spring Boot.
