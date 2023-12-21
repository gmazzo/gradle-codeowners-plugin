package io.github.gmazzo.codeowners

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

open class CodeOwnersBasePlugin<Extension : CodeOwnersBaseExtension>(
    @Suppress("UNCHECKED_CAST")
    private val extensionClass: Class<out Extension> = CodeOwnersBaseExtension::class.java as Class<Extension>,
) : Plugin<Project> {

    companion object {
        const val TASK_GROUP = "CodeOwners"
    }

    open fun Project.configure(extension: Extension, parent: Extension?, defaultLocations: FileCollection) {}

    override fun apply(target: Project): Unit = with(target) {
        val parentExtension = generateSequence(parent) { it.parent }
            .mapNotNull { it.extensions.findByType(extensionClass) }
            .firstOrNull()

        with(extensions.create("codeOwners", extensionClass)) {

            rootDirectory
                .valueIfNotNull(parentExtension?.rootDirectory)
                .convention(layout.projectDirectory)
                .finalizeValueOnRead()

            val defaultLocations = files(
                "CODEOWNERS",
                ".github/CODEOWNERS",
                ".gitlab/CODEOWNERS",
                "docs/CODEOWNERS",
            )

            val defaultFile = lazy {
                defaultLocations.asFileTree.singleOrNull() ?: error(defaultLocations.joinToString(
                    prefix = "No CODEOWNERS file found! Default locations:\n",
                    separator = "\n"
                ) { "- ${it.toRelativeString(rootDir)}" })
            }

            codeOwnersFile
                .valueIfNotNull(parentExtension?.codeOwnersFile)
                .convention(layout.file(provider(defaultFile::value)))
                .finalizeValueOnRead()

            configure(this, parentExtension, defaultLocations)
        }

        afterEvaluate {
            val expectedPlugins = listOf(
                "io.github.gmazzo.codeowners.jvm",
                "io.github.gmazzo.codeowners.kotlin",
                "io.github.gmazzo.codeowners.report",
            )

            check(expectedPlugins.any(plugins::hasPlugin)) {
                expectedPlugins.joinToString(
                    prefix = "'io.github.gmazzo.codeowners' plugin is deprecated, use either ",
                    separator = " or ",
                    postfix = " instead",
                    transform = { "'$it'" }
                )
            }
        }
    }

    protected fun <Type, PropertyOfType : Property<Type>> PropertyOfType.valueIfNotNull(
        provider: Provider<Type>?,
    ): PropertyOfType = apply {
        if (provider != null) {
            value(provider)
        }
    }

}
