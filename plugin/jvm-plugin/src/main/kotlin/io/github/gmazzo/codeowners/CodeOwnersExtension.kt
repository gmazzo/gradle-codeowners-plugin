package io.github.gmazzo.codeowners

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

interface CodeOwnersExtension : CodeOwnersConfig {

     /**
      * The base path of the entries on the CODEOWNERS file
      */
     val rootDirectory : DirectoryProperty

     /**
      * The path to the CODEOWNERS file
      */
     val codeOwnersFile : RegularFileProperty

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
