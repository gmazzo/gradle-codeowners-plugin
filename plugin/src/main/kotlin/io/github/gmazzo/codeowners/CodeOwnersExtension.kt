package io.github.gmazzo.codeowners

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

interface CodeOwnersExtension {

     val rootDirectory : DirectoryProperty

     val codeOwnersFile : RegularFileProperty

     val codeOwners : Property<CodeOwnersFile>

}
