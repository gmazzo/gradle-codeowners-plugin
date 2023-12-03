package io.github.gmazzo.codeowners

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

interface CodeOwnersExtension {

     /**
      * The base path of the entries on the CODEOWNERS file
      */
     val rootDirectory : DirectoryProperty

     /**
      * The path to the CODEOWNERS file
      */
     val codeOwnersFile : RegularFileProperty

     /**
      * If Kotlin classes runtime available (accessed from [io.github.gmazzo.codeowners.codeOwnersOf] API).
      *
      * If `false` only [CodeOwnersReportTask] tasks will be added to the projects.
      */
     val enableRuntimeSupport: Property<Boolean>

}
