package io.github.gmazzo.codeowners

import org.gradle.api.provider.Property

interface CodeOwnersJVMExtension : CodeOwnersBaseExtension, CodeOwnersConfig {

     /**
      * Where if code ownership info should be added as Java resources to be available at runtime.
      *
      * Turn this feature off to avoid polluting your production code, but keep in mind
      * [io.github.gmazzo.codeowners.codeOwnersOf] API won't work without it.
      */
     val addCodeOwnershipAsResources: Property<Boolean>

}
