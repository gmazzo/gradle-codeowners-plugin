package io.github.gmazzo.codeowners

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty

interface CodeOwnersExtension<SourceSet : CodeOwnersSourceSet> {

     /**
      * The base path of the entries on the CODEOWNERS file
      */
     val rootDirectory : DirectoryProperty

     /**
      * The path to the CODEOWNERS file
      */
     val codeOwnersFile : RegularFileProperty

     val sourceSets : NamedDomainObjectContainer<SourceSet>

}
