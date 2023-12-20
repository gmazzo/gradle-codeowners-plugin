package io.github.gmazzo.codeowners

import org.gradle.api.provider.Property

interface CodeOwnersExtension : CodeOwnersBaseExtension {

     /**
      * If Kotlin classes runtime available (accessed from [io.github.gmazzo.codeowners.codeOwnersOf] API).
      *
      * If `false` only [CodeOwnersReportTask] tasks will be added to the projects.
      */
     val enableRuntimeSupport: Property<Boolean>

}
