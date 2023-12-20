![GitHub](https://img.shields.io/github/license/gmazzo/gradle-codeowners-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.gmazzo.codeowners/core)](https://central.sonatype.com/artifact/io.github.gmazzo.codeowners/core)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.gmazzo.codeowners.jvm)](https://plugins.gradle.org/plugin/io.github.gmazzo.codeowners.jvm)
[![Build Status](https://github.com/gmazzo/gradle-codeowners-plugin/actions/workflows/build.yaml/badge.svg)](https://github.com/gmazzo/gradle-codeowners-plugin/actions/workflows/build.yaml)
[![Coverage](https://codecov.io/gh/gmazzo/gradle-codeowners-plugin/branch/main/graph/badge.svg?token=ExYkP1Q9oE)](https://codecov.io/gh/gmazzo/gradle-codeowners-plugin)

# gradle-codeowners-plugin
A Gradle plugin to propagate CODEOWNERS to JVM classes

# Usage
This plugin is designed to work as a whole build plugin, but you can selectively apply it to target the modules where you actually care about CODEOWNERS propagation.

The simplest setup is to apply the plugin at the root project, and then to each submodule. At root `build.gradle.kts` add:
```kotlin
plugins {
    id("io.github.gmazzo.codeowners.kotlin") version "<latest>"
}

subprojects {
    apply(plugin = "io.github.gmazzo.codeowners.kotlin")
}
```

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
    id("io.github.gmazzo.codeowners.kotlin") version "<latest>"
}

subprojects {
    apply(plugin = "io.github.gmazzo.codeowners.jvm")
}
```
You must apply the plugin on every project that has source files. Those classes won't be attributed otherwise.
Applying it at the root project only, will only make sense on single module builds.

## Disable it for specific `SourceSet`s (Java)
You can use the `sourceSet.codeOwners.enabled` property to configure it. 
For instance, the following code will disable it `test`:
```kotlin
sourceSets.test {
    codeOwners.enabled.set(false)
}
```

## Disable it for specific `Variant`s (Android)
You can use the `variant.codeOwners.enabled` property to configure it.
For instance, the following code will enable it only for `debug`:
```kotlin
androidComponents.onVariants {
    it.codeOwners.enabled.set(it.buildType == "debug")
}
```

## Consuming the generated `mappedCodeOwnersFile`
Each `CodeOwnersTask` produces a .CODEOWNERS like file which translates build directories to Java packages (in folder format, not '.').

To explain this better, given a `.CODEOWNERS` file:
```
src/main/java       @java-devs
src/main/kotlin     @koltin-devs
```
And some source files:
```
src/main/java/org/example/app/App.java
src/main/java/org/example/foo/Foo.java
src/main/kotlin/org/example/app/AppKt.kt
src/main/kotlin/org/example/bar/Bar.kt
```
The generated `mappedCodeOwnersFile` will contain
```
org/example/app     @koltin-devs @java-devs
org/example/foo     @java-devs
org/example/bar     @koltin-devs
```

You could use these files up feed any observability service for instance, allowing to have owners attributions given a package name/path.

In case you want to generate these but don't want to pollute/expose your production code with the ownership information, you use the `addCodeOwnershipAsResources` DSL to prevent the resources to be added (still generating the mapping files):
```kotlin
codeOwners.addCodeOwnershipAsResources.set(false)
```

To consume the `mappedCodeOwnersFile`, use the `.map`Property API, to ensure task dependencies are correctly computed, for instance:
```kotlin
tasks.register<Copy>("collectMappingFiles") {
    from(tasks.named("generateCodeOwnersResources").flatMap { it.mappedCodeOwnersFile })
    into(project.layout.buildDirectory.dir("mappings"))
}
```

## The CODEOWNERS file
The expected format is the same as [GitHub's](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners#codeowners-syntax) and it can be located at any of the following paths:
- `$rootDir/CODEOWNERS`
- `$rootDir/.github/CODEOWNERS`
- `$rootDir/.gitlab/CODEOWNERS`
- `$rootDir/docs/CODEOWNERS`

### Specifying CODEOWNERS file location
```kotlin
codeOwners.codeOwnersFile.set(layout.projectDirectory.file("somePath/.CODEOWNERS"))
```

# How it works
This plugin is mean to work at JVM level: compatible with `java`, `groovy` and `kotlin` and Android.

The plugin binds by default on the compilation toolchain, inspecting the source folders and generating a set of java `.codeowners` resources that later the provided `xxx.codeowners` function will use to resolve the ownership information (or `getCodeOwners` on Java, see [Usage](#usage))

## Caveats on the approach
The plugin will do its best to provide a reliable owners attribution on classes, even by inspecting dependencies looking for collision of packages.

For instance, multiple source folders
- `src/main/java/com/test/myapp/aaa`
- `src/main/kotlin/com/test/myapp/bbb`

may be contributing to the same `com.test.myapp` package.

If you `.CODEOWNERS` file looks similar to this:
```
aaaOwner    src/*/java
bbbOwner    src/*/kotlin
```
The final owners for classes located at the given package will be:

| pacakge              | owners                    |
|----------------------|---------------------------|
| `com.test.myapp`     | `aaaOwner` and `bbbOwner` |
| `com.test.myapp.aaa` | `aaaOwner`                |
| `com.test.myapp.bbb` | `bbbOwner`                |

## General advices for structuring CODEOWNERS file for this plugin
Given the known limitations on the JVM resources approach, here is a list tips to have a 100%  owners attributes accuracy:
1) Use a dedicated Java package per Gradle module
2) Prefer directory patterns over file extension ones
