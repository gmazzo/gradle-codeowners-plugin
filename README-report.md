# The `io.github.gmazzo.codeowners` plugin

Generates `codeOwnersReport` a task that produces a `.CODEOWNERS`-like file with class names:

```CODEOWNERS
# CodeOwners of module ':demo-project-jvm:app'

org/test/jvm/app/AppClass               app-devs
org/test/jvm/app/AppOwnersTest          test-devs
org/test/jvm/app/BuildConfig            app-devs
org/test/jvm/app/test/BuildConfig       app-devs
org/test/jvm/utils/AppUtils             app-devs
```

This plugin integrates with Java, Android and Kotlin plugins, generating dedicated `codeOwnersXXXReport` tasks per
source set/variant:

- For Java, creates a task per `SourceSet`
- For Android, creates a task per `Variant` (including their `UnitTest` and `AndroidTest`s)
- For Kotlin, creates a task per `KotlinSourceSet`. Supports any Kotlin subplugin: `jvm`, `android`, and `multiplatform`
