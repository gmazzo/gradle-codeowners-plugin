![GitHub](https://img.shields.io/github/license/gmazzo/gradle-codeowners-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.gmazzo.codeowners/core)](https://search.maven.org/artifact/io.github.gmazzo.codeowners/core)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.gmazzo.codeowners)](https://plugins.gradle.org/plugin/io.github.gmazzo.codeowners)
![Build Status](https://github.com/gmazzo/gradle-codeowners-plugin/actions/workflows/build.yaml/badge.svg)
[![Coverage](https://codecov.io/gh/gmazzo/gradle-codeowners-plugin/branch/main/graph/badge.svg?token=ExYkP1Q9oE)](https://codecov.io/gh/gmazzo/gradle-codeowners-plugin)

# gradle-codeowners-plugin
A Gradle plugin to propagate CODEOWNERS to JVM classes

# Usage
Apply the plugin at the **root** project and/or at **any child** project that uses it:
```kotlin
plugins {
    id("io.github.gmazzo.codeowners") version "<latest>"
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
    id("io.github.gmazzo.codeowners") version "<latest>"
}

subprojects {
    apply(plugin = "io.github.gmazzo.codeowners")
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

## Excluding default `io.github.gmazzo.codeowners:core` dependency
By default, a dependency to this plugin runtime DSL (the `.codeOwners` functions) will be added to those `SourceSet`s where CodeOwners are computed.

You can opt out of this behavior by adding `codeowners.default.dependency=false` to your `gradle.properties` file and then manually add it on the `Configuration` that fits better for your build:
```kotlin
dependencies {
    implementation("io.github.gmazzo.codeowners:core")
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
