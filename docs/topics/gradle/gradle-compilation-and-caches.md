[//]: # (title: Compilation and caches in the Kotlin Gradle plugin)

On this page, you can learn about the following topics:
* [Incremental compilation](#incremental-compilation)
* [Gradle build cache support](#gradle-build-cache-support)
* [Gradle configuration cache support](#gradle-configuration-cache-support)
* [The Kotlin daemon and how to use it with Gradle](#the-kotlin-daemon-and-how-to-use-it-with-gradle)
* [Rolling back to the previous compiler](#rolling-back-to-the-previous-compiler)
* [Defining Kotlin compiler execution strategy](#defining-kotlin-compiler-execution-strategy)
* [Kotlin compiler fallback strategy](#kotlin-compiler-fallback-strategy)
* [Trying the latest language version](#trying-the-latest-language-version)
* [Build reports](#build-reports)

## Incremental compilation

The Kotlin Gradle plugin supports incremental compilation, which is enabled by default for Kotlin/JVM and Kotlin/JS projects.
Incremental compilation tracks changes to files in the classpath between builds so that only the files affected
by these changes are compiled.
This approach works with [Gradle's build cache](#gradle-build-cache-support) and supports [compilation avoidance](https://docs.gradle.org/current/userguide/java_plugin.html#sec:java_compile_avoidance).

For Kotlin/JVM, incremental compilation relies on classpath snapshots,
which capture the API structure of modules to determine when recompilation is necessary.
To optimize the overall pipeline, the Kotlin compiler uses two types of classpath snapshots:

* **Fine-grained snapshots:** include detailed information about class members, such as properties or functions.
When member-level changes are detected, the Kotlin compiler recompiles only the classes that depend on the modified members.
To maintain performance, the Kotlin Gradle plugin creates coarse-grained snapshots for `.jar` files in the Gradle cache.
* **Coarse-grained snapshots:** only contain the class [ABI](https://en.wikipedia.org/wiki/Application_binary_interface) hash.
When a part of ABI changes, the Kotlin compiler recompiles all classes that depend on the changed class.
This is useful for classes that change infrequently, such as external libraries.

> Kotlin/JS projects use a different incremental compilation approach based on history files. 
>
{style="note"}

There are several ways to disable incremental compilation:

* Set `kotlin.incremental=false` for Kotlin/JVM.
* Set `kotlin.incremental.js=false` for Kotlin/JS projects.
* Use `-Pkotlin.incremental=false` or `-Pkotlin.incremental.js=false` as a command line parameter.

  The parameter should be added to each subsequent build.

When you disable incremental compilation, incremental caches become invalid after the build. The first build is never incremental.

> Sometimes problems with incremental compilation become visible several rounds after the failure occurs. Use [build reports](#build-reports)
> to track the history of changes and compilations. This can help you to provide reproducible bug reports.
>
{style="tip"}

To learn more about how our current incremental compilation approach works and compares to the previous one,
see our [blog post](https://blog.jetbrains.com/kotlin/2022/07/a-new-approach-to-incremental-compilation-in-kotlin/).

## Gradle build cache support

The Kotlin plugin uses the [Gradle build cache](https://docs.gradle.org/current/userguide/build_cache.html), which stores
the build outputs for reuse in future builds.

To disable caching for all Kotlin tasks, set the system property `kotlin.caching.enabled` to `false`
(run the build with the argument `-Dkotlin.caching.enabled=false`).

## Gradle configuration cache support

The Kotlin plugin uses the [Gradle configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html),
which speeds up the build process by reusing the results of the configuration phase for subsequent builds.

See the [Gradle documentation](https://docs.gradle.org/current/userguide/configuration_cache.html#config_cache:usage)
to learn how to enable the configuration cache. After you enable this feature, the Kotlin Gradle plugin automatically
starts using it.

## The Kotlin daemon and how to use it with Gradle

The Kotlin daemon:
* Runs with the Gradle daemon to compile the project.
* Runs separately from the Gradle daemon when you compile the project with an IntelliJ IDEA built-in build system.

The Kotlin daemon starts at the Gradle [execution stage](https://docs.gradle.org/current/userguide/build_lifecycle.html#sec:build_phases)
when one of the Kotlin compile tasks starts to compile sources.
The Kotlin daemon stops either with the Gradle daemon or after two idle hours with no Kotlin compilation.

The Kotlin daemon uses the same JDK that the Gradle daemon does.

### Setting Kotlin daemon's JVM arguments

Each of the following ways to set arguments overrides the ones that came before it:
* [Gradle daemon arguments inheritance](#gradle-daemon-arguments-inheritance)
* [`kotlin.daemon.jvm.options` system property](#kotlin-daemon-jvm-options-system-property)
* [`kotlin.daemon.jvmargs` property](#kotlin-daemon-jvmargs-property)
* [`kotlin` extension](#kotlin-extension)
* [Specific task definition](#specific-task-definition)

#### Gradle daemon arguments inheritance

By default, the Kotlin daemon inherits a specific set of arguments from the Gradle daemon but overwrites them with any 
JVM arguments specified directly for the Kotlin daemon. For example, if you add the following JVM arguments in the `gradle.properties` file:

```none
org.gradle.jvmargs=-Xmx1500m -Xms500m -XX:MaxMetaspaceSize=1g
```

These arguments are then added to the Kotlin daemon's JVM arguments:

```none
-Xmx1500m -XX:ReservedCodeCacheSize=320m -XX:MaxMetaspaceSize=1g -XX:UseParallelGC -ea -XX:+UseCodeCacheFlushing -XX:+HeapDumpOnOutOfMemoryError -Djava.awt.headless=true -Djava.rmi.server.hostname=127.0.0.1 --add-exports=java.base/sun.nio.ch=ALL-UNNAMED
```

> To learn more about the Kotlin daemon's default behavior with JVM arguments, see [Kotlin daemon's behavior with JVM arguments](#kotlin-daemon-s-behavior-with-jvm-arguments).
>
{style="note"}

#### kotlin.daemon.jvm.options system property

If the Gradle daemon's JVM arguments have the `kotlin.daemon.jvm.options` system property – use it in the `gradle.properties` file:

```none
org.gradle.jvmargs=-Dkotlin.daemon.jvm.options=-Xmx1500m,Xms500m
```

When passing arguments, follow these rules:
* Use the minus sign `-` **only** before the arguments `Xmx`, `XX:MaxMetaspaceSize`, and `XX:ReservedCodeCacheSize`.
* Separate arguments with commas (`,`) _without_ spaces. Arguments that come after a space will be used for the Gradle daemon, not for the Kotlin daemon.

> Gradle ignores these properties if all the following conditions are satisfied:
> * Gradle is using JDK 1.9 or higher.
> * The version of Gradle is between 7.0 and 7.1.1 inclusively.
> * Gradle is compiling Kotlin DSL scripts.
> * The Kotlin daemon isn't running.
>
> To overcome this, upgrade Gradle to the version 7.2 (or higher) or use the `kotlin.daemon.jvmargs` property – see the following section.
>
{style="warning"}

#### kotlin.daemon.jvmargs property

You can add the `kotlin.daemon.jvmargs` property in the `gradle.properties` file:

```none
kotlin.daemon.jvmargs=-Xmx1500m -Xms500m
```

Note that if you don't specify the `ReservedCodeCacheSize` argument here or in Gradle's JVM arguments, the Kotlin Gradle plugin applies a default value of `320m`:

```none
-Xmx1500m -XX:ReservedCodeCacheSize=320m -Xms500m
```

#### kotlin extension

You can specify arguments in the `kotlin` extension:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
kotlin {
    kotlinDaemonJvmArgs = listOf("-Xmx486m", "-Xms256m", "-XX:+UseParallelGC")
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
kotlin {
    kotlinDaemonJvmArgs = ["-Xmx486m", "-Xms256m", "-XX:+UseParallelGC"]
}
```

</tab>
</tabs>

#### Specific task definition

You can specify arguments for a specific task:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
tasks.withType<CompileUsingKotlinDaemon>().configureEach {
    kotlinDaemonJvmArguments.set(listOf("-Xmx486m", "-Xms256m", "-XX:+UseParallelGC"))
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
tasks.withType(CompileUsingKotlinDaemon).configureEach { task ->
    task.kotlinDaemonJvmArguments = ["-Xmx1g", "-Xms512m"]
}
```

</tab>
</tabs>

> In this case a new Kotlin daemon instance can start on task execution. Learn more about [Kotlin daemon's behavior with JVM arguments](#kotlin-daemon-s-behavior-with-jvm-arguments).
>
{style="note"}

### Kotlin daemon's behavior with JVM arguments

When configuring the Kotlin daemon's JVM arguments, note that:

* It is expected to have multiple instances of the Kotlin daemon running at the same time when different subprojects or tasks have different sets of JVM arguments.
* A new Kotlin daemon instance starts only when Gradle runs a related compilation task and existing Kotlin daemons do not have the same set of JVM arguments.
  Imagine that your project has a lot of subprojects. Most of them require some heap memory for a Kotlin daemon, but one module requires a lot (though it is rarely compiled).
  In this case, you should provide a different set of JVM arguments for such a module, so a Kotlin daemon with a larger heap size would start only for developers who touch this specific module.
  > If you are already running a Kotlin daemon that has enough heap size to handle the compilation request,
  > even if other requested JVM arguments are different, this daemon will be reused instead of starting a new one.
  >
  {style="note"}

If the following arguments aren't specified, the Kotlin daemon inherits them from the Gradle daemon:

* `-Xmx`
* `-XX:MaxMetaspaceSize`
* `-XX:ReservedCodeCacheSize`. If not specified or inherited, the default value is `320m`.

The Kotlin daemon has the following default JVM arguments:
* `-XX:UseParallelGC`. This argument is only applied if no other garbage collector is specified.
* `-ea`
* `-XX:+UseCodeCacheFlushing`
* `-Djava.awt.headless=true`
* `-D{java.servername.property}={localhostip}`
* `--add-exports=java.base/sun.nio.ch=ALL-UNNAMED`. This argument is only applied for JDK versions 16 or higher.

> The list of default JVM arguments for the Kotlin daemon may vary between versions. You can use a tool like [VisualVM](https://visualvm.github.io/) to check the actual settings of a running JVM process, like the Kotlin daemon.
>
{style="note"}

## Rolling back to the previous compiler

From Kotlin 2.0.0, the K2 compiler is used by default.

To use the previous compiler from Kotlin 2.0.0 onwards, either:

* In your `build.gradle.kts` file, [set your language version](gradle-compiler-options.md#example-of-setting-languageversion) to `1.9`.

  OR
* Use the following compiler option: `-language-version 1.9`.

To learn more about the benefits of the K2 compiler, see the [K2 compiler migration guide](k2-compiler-migration-guide.md).

## Defining Kotlin compiler execution strategy

_Kotlin compiler execution strategy_ defines where the Kotlin compiler is executed and if incremental compilation is supported in each case.

There are three compiler execution strategies:

| Strategy       | Where Kotlin compiler is executed          | Incremental compilation | Other characteristics and notes                                                                                                                                                                                                                                                |
|----------------|--------------------------------------------|-------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Daemon         | Inside its own daemon process              | Yes                     | _The default and fastest strategy_. Can be shared between different Gradle daemons and multiple parallel compilations.                                                                                                                                                         |
| In process     | Inside the Gradle daemon process           | No                      | May share the heap with the Gradle daemon. The "In process" execution strategy is _slower_ than the "Daemon" execution strategy. Each [worker](https://docs.gradle.org/current/userguide/worker_api.html) creates a separate Kotlin compiler classloader for each compilation. |
| Out of process | In a separate process for each compilation | No                      | The slowest execution strategy. Similar to the "In process", but additionally creates a separate Java process within a Gradle worker for each compilation.                                                                                                                     |

To define a Kotlin compiler execution strategy, you can use one of the following properties:
* The `kotlin.compiler.execution.strategy` Gradle property.
* The `compilerExecutionStrategy` compile task property.

The task property `compilerExecutionStrategy` takes priority over the Gradle property `kotlin.compiler.execution.strategy`.

The available values for the `kotlin.compiler.execution.strategy` property are:
1. `daemon` (default)
2. `in-process`
3. `out-of-process`

Use the Gradle property `kotlin.compiler.execution.strategy` in `gradle.properties`:

```none
kotlin.compiler.execution.strategy=out-of-process
```

The available values for the `compilerExecutionStrategy` task property are:
1. `org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy.DAEMON` (default)
2. `org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy.IN_PROCESS`
3. `org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy.OUT_OF_PROCESS`

Use the task property `compilerExecutionStrategy` in your build scripts:

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
import org.jetbrains.kotlin.gradle.tasks.CompileUsingKotlinDaemon
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy

// ...

tasks.withType<CompileUsingKotlinDaemon>().configureEach {
    compilerExecutionStrategy.set(KotlinCompilerExecutionStrategy.IN_PROCESS)
} 
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
import org.jetbrains.kotlin.gradle.tasks.CompileUsingKotlinDaemon
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy

// ...

tasks.withType(CompileUsingKotlinDaemon)
    .configureEach {
        compilerExecutionStrategy = KotlinCompilerExecutionStrategy.IN_PROCESS
    }
```

</tab>
</tabs>

## Kotlin compiler fallback strategy

The Kotlin compiler's fallback strategy is to run a compilation outside a Kotlin daemon if the daemon somehow fails. 
If the Gradle daemon is on, the compiler uses the ["In process" strategy](#defining-kotlin-compiler-execution-strategy). 
If the Gradle daemon is off, the compiler uses the "Out of process" strategy.

When this fallback happens, you have the following warning lines in your Gradle's build output:

```none
Failed to compile with Kotlin daemon: java.lang.RuntimeException: Could not connect to Kotlin compile daemon
[exception stacktrace]
Using fallback strategy: Compile without Kotlin daemon
Try ./gradlew --stop if this issue persists.
```

However, a silent fallback to another strategy can consume a lot of system resources or lead to non-deterministic builds. 
Read more about this in this [YouTrack issue](https://youtrack.jetbrains.com/issue/KT-48843/Add-ability-to-disable-Kotlin-daemon-fallback-strategy).
To avoid this, there is a Gradle property `kotlin.daemon.useFallbackStrategy`, whose default value is `true`. 
When the value is `false`, builds fail on problems with the daemon's startup or communication. Declare this property in
`gradle.properties`:

```none
kotlin.daemon.useFallbackStrategy=false
```

There is also a `useDaemonFallbackStrategy` property in Kotlin compile tasks, which takes priority over the Gradle property if you use both. 

<tabs group="build-script">
<tab title="Kotlin" group-key="kotlin">

```kotlin
tasks {
    compileKotlin {
        useDaemonFallbackStrategy.set(false)
    }   
}
```

</tab>
<tab title="Groovy" group-key="groovy">

```groovy
tasks.named("compileKotlin").configure {
    useDaemonFallbackStrategy = false
}
```
</tab>
</tabs>

If there is insufficient memory to run the compilation, you can see a message about it in the logs.

## Trying the latest language version

Starting with Kotlin 2.0.0, to try the latest language version, set the `kotlin.experimental.tryNext` property in your `gradle.properties`
file. When you use this property, the Kotlin Gradle plugin increments the language version to one above the default value
for your Kotlin version. For example, in Kotlin 2.0.0, the default language version is 2.0, so the property configures 
language version 2.1.

Alternatively, you can run the following command:

```shell
./gradlew assemble -Pkotlin.experimental.tryNext=true
``` 

In [build reports](#build-reports), you can find the language version used to compile each task.

## Build reports

Build reports contain the durations of different compilation phases and any reasons why compilation couldn't be incremental.
Use build reports to investigate performance issues when the compilation time is too long or when it differs for the same
project.

Kotlin build reports help you to investigate problems with build performance more efficiently than with [Gradle build scans](https://scans.gradle.com/)
that have a single Gradle task as the unit of granularity.

There are two common cases that analyzing build reports for long-running compilations can help you resolve:
* The build wasn't incremental. Analyze the reasons and fix underlying problems.
* The build was incremental but took too much time. Try reorganizing source files — split big files,
  save separate classes in different files, refactor large classes, declare top-level functions in different files, and so on.

Build reports also show the Kotlin version used in the project. In addition, starting with Kotlin 1.9.0,
you can see which compiler was used to compile the code in your [Gradle build scans](https://scans.gradle.com/).

Learn [how to read build reports](https://blog.jetbrains.com/kotlin/2022/06/introducing-kotlin-build-reports/#how_to_read_build_reports) 
and about [how JetBrains uses build reports](https://blog.jetbrains.com/kotlin/2022/06/introducing-kotlin-build-reports/#how_we_use_build_reports_in_jetbrains).

### Enabling build reports

To enable build reports, declare where to save the build report output in `gradle.properties`:

```none
kotlin.build.report.output=file
```

The following values and their combinations are available for the output:

| Option | Description |
|---|---|
| `file` | Saves build reports in a human-readable format to a local file. By default, it's `${project_folder}/build/reports/kotlin-build/${project_name}-timestamp.txt` |
| `single_file` | Saves build reports in a format of an object to a specified local file. |
| `build_scan` | Saves build reports in the `custom values` section of the [build scan](https://scans.gradle.com/). Note that the Gradle Enterprise plugin limits the number of custom values and their length. In big projects, some values could be lost. |
| `http` | Posts build reports using HTTP(S). The POST method sends metrics in JSON format. You can see the current version of the sent data in the [Kotlin repository](https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/report/data/GradleCompileStatisticsData.kt). You can find samples of HTTP endpoints in [this blog post](https://blog.jetbrains.com/kotlin/2022/06/introducing-kotlin-build-reports/#enable_build_reports) |
| `json` | Saves build reports in JSON format to a local file. Set the location for your build reports in `kotlin.build.report.json.directory` (see below). By default, it's name is `${project_name}-build-<date-time>-<index>.json`. |

Here's a list of available options for `kotlin.build.report`:

```none
# Required outputs. Any combination is allowed
kotlin.build.report.output=file,single_file,http,build_scan,json

# Mandatory if single_file output is used. Where to put reports 
# Use instead of the deprecated `kotlin.internal.single.build.metrics.file` property
kotlin.build.report.single_file=some_filename

# Mandatory if json output is used. Where to put reports 
kotlin.build.report.json.directory=my/directory/path

# Optional. Output directory for file-based reports. Default: build/reports/kotlin-build/
kotlin.build.report.file.output_dir=kotlin-reports

# Optional. Label for marking your build report (for example, debug parameters)
kotlin.build.report.label=some_label
```

Options, applicable only to HTTP:

```none
# Mandatory. Where to post HTTP(S)-based reports
kotlin.build.report.http.url=http://127.0.0.1:8080

# Optional. User and password if the HTTP endpoint requires authentication
kotlin.build.report.http.user=someUser
kotlin.build.report.http.password=somePassword

# Optional. Add a Git branch name of a build to a build report
kotlin.build.report.http.include_git_branch.name=true|false

# Optional. Add compiler arguments to a build report
# If a project contains many modules, its compiler arguments in the report can be very heavy and not that helpful
kotlin.build.report.include_compiler_arguments=true|false
```

### Limit of custom values

To collect build scans' statistics, Kotlin build reports use [Gradle's custom values](https://docs.gradle.com/enterprise/tutorials/extending-build-scans/). 
Both you and different Gradle plugins can write data to custom values. The number of custom values has a limit.
See the current maximum custom value count in the [Build scan plugin docs](https://docs.gradle.com/enterprise/gradle-plugin/#adding_custom_values).

If you have a big project, a number of such custom values may be quite big. If this number exceeds the limit, 
you can see the following message in the logs:

```text
Maximum number of custom values (1,000) exceeded
```

To reduce the number of custom values the Kotlin plugin produces, you can use the following property in `gradle.properties`:

```none
kotlin.build.report.build_scan.custom_values_limit=500
```

### Switching off collecting project and system properties

HTTP build statistic logs can contain some project and system properties. These properties can change builds' behavior, 
so it's useful to log them in build statistics. 
These properties can store sensitive data, for example, passwords or a project's full path.

You can disable collection of these statistics by adding the `kotlin.build.report.http.verbose_environment` property to
your `gradle.properties`.

> JetBrains doesn't collect these statistics. You choose a place [where to store your reports](#enabling-build-reports).
> 
{style="note"}

## What's next?

Learn more about:
* [Gradle basics and specifics](https://docs.gradle.org/current/userguide/userguide.html).
* [Support for Gradle plugin variants](gradle-plugin-variants.md).
