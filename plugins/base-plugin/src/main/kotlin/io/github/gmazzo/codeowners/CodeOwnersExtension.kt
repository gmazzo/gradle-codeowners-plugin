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

     /**
      * The collecting of CodeOwners source sets.
      * This is usually a mirror of:
      * - [SourceSet]s for Java projects
      * - [com.android.build.api.variant.Component]s variants (include test and androidTest) for Android projects
      * - [org.jetbrains.kotlin.gradle.plugin.KotlinCompilation]s for Kotlin projects
      */
     val sourceSets : NamedDomainObjectContainer<SourceSet>

}
