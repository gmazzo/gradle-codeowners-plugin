![GitHub](https://img.shields.io/github/license/gmazzo/gradle-codeowners-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.gmazzo.codeowners/io.github.gmazzo.codeowners.gradle.plugin)](https://central.sonatype.com/artifact/io.github.gmazzo.codeowners/io.github.gmazzo.codeowners.gradle.plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.gmazzo.codeowners)](https://plugins.gradle.org/plugin/io.github.gmazzo.codeowners)
[![Build Status](https://github.com/gmazzo/gradle-codeowners-plugin/actions/workflows/ci-cd.yaml/badge.svg)](https://github.com/gmazzo/gradle-codeowners-plugin/actions/workflows/ci-cd.yaml)
[![Coverage](https://codecov.io/gh/gmazzo/gradle-codeowners-plugin/branch/main/graph/badge.svg?token=ExYkP1Q9oE)](https://codecov.io/gh/gmazzo/gradle-codeowners-plugin)
[![Users](https://img.shields.io/badge/users_by-Sourcegraph-purple)](https://sourcegraph.com/search?q=content:io.github.gmazzo.codeowners+-repo:github.com/gmazzo/gradle-codeowners-plugin)

# gradle-codeowners-plugin
A Gradle plugin to propagate CODEOWNERS information to classes

It consists on 3 different plugins, each one providing a different set of features and targeting different use cases:
1) [The `io.github.gmazzo.codeowners` plugin](README-report.md) adds a Gradle report of classes' ownership
2) [The `io.github.gmazzo.codeowners.jvm` plugin](README-jvm.md) propagates the classes' ownership information to runtime. It supports any JVM build (`java`, `groovy`, etc) that produces `.class` files, but **JVM-only**
3) [The `io.github.gmazzo.codeowners.kotlin` Kotlin Compiler plugin](README-kotlin.md) propagates the classes' ownership information to runtime. It supports any Kotlin build (`jvm`, `android`, `multiplatform`, etc, but **Kotlin-only**)

# Usage
This plugin is designed to work as a whole build plugin, but you can selectively apply it to target the modules where you actually care about CODEOWNERS propagation.

The simplest setup is to apply the plugin at the root project, and then to each submodule. At root `build.gradle.kts` add:
```kotlin
plugins {
    id("io.github.gmazzo.codeowners") version "<latest>" 
}

subprojects {
    apply(plugin = "io.github.gmazzo.codeowners")
}
```

You should apply the right plugins according to your needs:
- `io.github.gmazzo.codeowners` (_Report_) is for the generating codeowners-like file by Java/Kotlin classes names
- `io.github.gmazzo.codeowners.jvm` (_JVM_) is for propagating codeowners info to JVM-only projects
- `io.github.gmazzo.codeowners.kotlin` (_Kotlin_) (recommended) is for propagating codeowners info to for **pure Kotlin** projects (including JVM and multiplatform)

| Plugin / Feature                                                     | _Report_ | _JVM_ | _Kotlin_ |
|----------------------------------------------------------------------|----------|-------|----------|
| Generates class-like reports at build time                           | ✅       | ✅ *  | ✅ *     |
| Propagates codeowners info to runtime                                | ❌       | ✅    | ✅       |
| Works with JVM projects                                              | ✅       | ✅    | ✅       |
| Works with Multiplatform projects                                    | ❌       | ❌    | ✅       |
| Acurrancy:<br/>Codeowners info matches always the original file ones | 🟢       | 🟡 ** | 🟢       |

(*) inherited from `io.github.gmazzo.codeowners` (_Report_) plugin<br/>
(**) because how the Java Resources API on JVM the ownership information may be inaccurate in some cases. See [Caveats on the approach](./README-jvm.md#caveats-on-the-approach) for more details.

## Getting ownership information at runtime
Later, you can query a class's owner by:
```kotlin
val ownersOfFoo = codeOwnersOf<Foo>()
```
or in Java:
```java
Set<String> ownersOfFoo = CodeOwners.getCodeOwners(Foo.class);
```

## Crash attribution
`Expection`s can also be attributed by inspecting its stacktrace (first match wins)
```kotlin
try {
    // do some work
    
} catch (ex: Throwable) {
    val ownersOfErr = ex.codeOwners
    // report to its owner
}
```
or in Java:
```java
try {
    // do some work

} catch (Throwable ex) {
    Set<String> ownersOfErr = CodeOwners.getCodeOwners(ex);
    // report to its owner
}
```

## Recommended setup on multi module projects
At root's `build.gradle.kts` add:
```kotlin
plugins {
    id("io.github.gmazzo.codeowners") version "<latest>"
}

subprojects {
    apply(plugin = "io.github.gmazzo.codeowners")
}
```
You must apply the plugin on **every project that has source files**. Those classes won't be computed otherwise.
Applying it at the root project only, will only make sense on single module builds.

# The CODEOWNERS file
The expected format is the same as [GitHub's](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners#codeowners-syntax) and it can be located at any of the following paths:
- `$rootDir/CODEOWNERS`
- `$rootDir/.github/CODEOWNERS`
- `$rootDir/.gitlab/CODEOWNERS`
- `$rootDir/docs/CODEOWNERS`

### Specifying CODEOWNERS file location
```kotlin
codeOwners.codeOwnersFile.set(layout.projectDirectory.file("somePath/.CODEOWNERS"))
```
