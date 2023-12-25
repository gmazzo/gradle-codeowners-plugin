# The `io.github.gmazzo.codeowners.jvm` plugin
Propagates CODEOWNERS to Kotlin classes, by generating a `@CodeOwners` annotation on each Kotlin Class/File at compilation time.

> [!NOTE]
> This plugin aggregates the `io.github.gmazzo.codeowners` plugin, so you don't need to apply it too.

# How it works
When applies, it binds a Kotlin IR compiler plugin that hooks on the compilation process, and it will decorate all processed Kotlin classes/files with a `@CodeOwners` (or `@file:CodeOwners`) annotation.

You can later query its ownership information as described at [Usage](./README.md#usage).

## Disable it for specific `KotlinTarget`s
You can use the `codeOwners.enabled` property to configure it.
For instance, the following code will disable it for the `jvm` target:
```kotlin
kotlin.targets.named("jvm") {
    codeOwners.enabled = false
}
```

## Disable it for specific `KotlinCompilation`s
You can use the `codeOwners.enabled` property to configure it.
For instance, the following code will disable it for any non-`main` compilation of any `KotlinTarget` target:
```kotlin
kotlin.targets.configureEach {
    compilations.configureEach {
        codeOwners.enabled = name == "main"
    }
}
```

> [!NOTE]
> It supports any Kotlin subplugin: `jvm`, `android`, including `multiplatform` ones targeting `native` or `js` platforms.
