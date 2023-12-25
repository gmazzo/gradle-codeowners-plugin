# The `io.github.gmazzo.codeowners.jvm` plugin
Propagates CODEOWNERS to JVM classes, by generating a package-level `.codeowners` resource files.

> [!NOTE]
> This plugin aggregates the `io.github.gmazzo.codeowners` plugin, so you don't need to apply it too.

# How it works
This plugin is mean to work at JVM level: compatible with `java`, `groovy` and `kotlin` and Android.

The plugin binds by default on the compilation toolchain, inspecting the source folders and generating a set of java `.codeowners` resources that later the provided `xxx.codeowners` function will use to resolve the ownership information (or `getCodeOwners` on Java, see [Usage](#usage))

You can later query its ownership information as described at [Usage](./README.md#usage).

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

## Disable it for specific `SourceSet`s (Java)
You can use the `codeOwners.enabled` property to configure it.
For instance, the following code will disable it for the `test` source set:
```kotlin
sourceSets.test {
    codeOwners.enabled = false
}
```

## Disable it for specific `Variant`s (Android)
You can use the `codeOwners.enabled` property to configure it.
For instance, the following code will enable it only for variants of `debug` build type:
```kotlin
androidComponents.onVariants { variant ->
    variant.codeOwners.enabled = false
}
```
