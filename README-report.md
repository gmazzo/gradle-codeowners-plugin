# The `io.github.gmazzo.codeowners` plugin

The a `codeOwnersReport` task can produce several reports and optionally enforcing ownership attribution checks:
- `mappings`: produces a `.CODEOWNERS`-like file with class names
- `xml`: produces an ownership report (including unowned class files) in XML format.
- `html`: produces an ownership report (including unowned class files) in HTML format.
- `checkstyle`: produces an unowned class files report in [Checkstyle](https://checkstyle.sourceforge.io/) compatible format (useful for CIs).
- `sarif`: produces an unowned class files report in [SARIF](https://sarifweb.azurewebsites.net/) format (useful for IDEs and CIs).

The reports can be configured through the DSL as:
```kotlin
codeOwners {
  reports {
    failOnUnowned() // fails if any unowned class file is found
    failOnUnownedThreshold = 10 // fail more than 10% of class files are unowned
    ignoreUnowned() // the task will never fail, but reports will still be produced (default)

    mappings.required = true // enables mappings report (enabled by default)
    xml.required = true // enables XML report (enabled by default)
    html.required = true // enables HTML report (enabled by default)
    checkstyle.required = false // enables Checkstyle report (disabled by default)
    sarif.required = false // enables SARIF report (disabled by default)
  }
}
```

This plugin integrates with Java, Android and Kotlin plugins, generating dedicated `codeOwnersXXXReport` tasks per
source set/variant:

- For Java, creates a task per `SourceSet`
- For Android, creates a task per `Variant` (including their `UnitTest` and `AndroidTest`s)
- For Kotlin, creates a task per `KotlinSourceSet`. Supports any Kotlin subplugin: `jvm`, `android`, and `multiplatform`

## The `mappings.codeowners` file format
The mappings file consists in a `.CODEOWNERS`-like file but with class names instead of file paths. i.e.:

```CODEOWNERS
# CodeOwners of module ':demo-project-jvm:app'

org/test/jvm/app/AppClass               app-devs
org/test/jvm/app/AppOwnersTest          test-devs
org/test/jvm/app/BuildConfig            app-devs
org/test/jvm/app/test/BuildConfig       app-devs
org/test/jvm/utils/AppUtils             app-devs
```
