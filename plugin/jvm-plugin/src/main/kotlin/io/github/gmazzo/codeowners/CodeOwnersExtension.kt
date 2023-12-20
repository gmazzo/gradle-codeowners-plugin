package io.github.gmazzo.codeowners

import org.gradle.api.provider.Property

interface CodeOwnersExtension : CodeOwnersBaseExtension, CodeOwnersConfig {

     /**
      * Where if code ownership info should be added as Java resources to be available at runtime.
      *
      * Turn this feature off to avoid polluting your production code, but keep in mind
      * [io.github.gmazzo.codeowners.codeOwnersOf] API won't work without it.
      */
     val addCodeOwnershipAsResources: Property<Boolean>

     /**
      * Where if `core` dependency (providing the [io.github.gmazzo.codeowners.codeOwnersOf] API) should be added to
      * the `implementation` classpath.
      *
      * Defaults to `codeowners.default.dependency` Gradle property (`true` if missing).
      * The dependency won't be added if [addCodeOwnershipAsResources] is set to `false` regardless the value of this property
      */
     val addCoreDependency: Property<Boolean>

}
