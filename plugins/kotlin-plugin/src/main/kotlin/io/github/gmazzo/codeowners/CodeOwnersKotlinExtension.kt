package io.github.gmazzo.codeowners

import org.gradle.api.provider.Property

interface CodeOwnersKotlinExtension : CodeOwnersBaseExtension {

     /**
      * If Kotlin classes runtime available (accessed from [io.github.gmazzo.codeowners.codeOwnersOf] API).
      *
      * If `false` only [CodeOwnersReportTask] tasks will be added to the projects.
      *
      * Basically, this flag makes sense if you only want to use this plugin to get a codeowners attribution file
      * about your classes, without making any instrumentation to them (left unchanged).
      */
     val enableRuntimeSupport: Property<Boolean>

}
