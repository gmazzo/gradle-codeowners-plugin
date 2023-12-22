package io.github.gmazzo.codeowners

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty

interface CodeOwnersExtension {

     /**
      * The base path of the entries on the CODEOWNERS file
      */
     val rootDirectory : DirectoryProperty

     /**
      * The path to the CODEOWNERS file
      */
     val codeOwnersFile : RegularFileProperty

}
