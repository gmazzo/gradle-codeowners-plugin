package com.github.gmazzo.codeowners

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

interface CodeOwnersExtension {

     val codeOwnersRoot : DirectoryProperty

     val codeOwnersFile : ConfigurableFileCollection

     val codeOwners : Property<CodeOwnersFile>

}
