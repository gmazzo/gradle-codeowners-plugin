package com.github.gmazzo.codeowners

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceDirectorySet
import com.github.gmazzo.codeowners.plugin.BuildConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JvmEcosystemPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.*

class CodeOwnersPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        rootProject.apply<CodeOwnersRootPlugin>()
        apply<JvmEcosystemPlugin>()

        val extension = rootProject.extensions.getByType<CodeOwnersExtension>()

        val sourceSets = objects.domainObjectContainer(CodeOwnersSourceSet::class) { name ->
            val ss = objects.sourceDirectorySet(name, "$name codeOwners sources")

            val prefix = when (name) {
                SourceSet.MAIN_SOURCE_SET_NAME -> ""
                else -> name.capitalized()
            }

            val generateTask = tasks.register<CodeOwnersTask>("generate${prefix}CodeOwnersResources") {
                codeOwners.value(extension.codeOwners)
                rootDirectory.value(extension.rootDirectory)
                sourceFiles.from(ss)
            }

            ss.destinationDirectory.value(layout.buildDirectory.dir("codeOwners/${ss.name}"))
            ss.compiledBy(generateTask, CodeOwnersTask::outputDirectory)

            CodeOwnersSourceSet(ss, generateTask)
        }

        the<SourceSetContainer>().configureEach { ss ->
            val sources = sourceSets.maybeCreate(ss.name)
            sources.source(ss.java)
            ss.resources.srcDir(sources.generateTask)
            ss.extensions.add(SourceDirectorySet::class.java, "codeOwners", sources.sources)

            plugins.withId("kotlin") {
                val kotlin: SourceDirectorySet by ss.extensions

                sources.source(kotlin)
            }

            plugins.withId("groovy") {
                val groovy: SourceDirectorySet by ss.extensions

                sources.source(groovy)
            }

        }

        plugins.withId("java") {
            dependencies {
                JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME(BuildConfig.CORE_DEPENDENCY)
            }
        }

        plugins.withId("com.android.base") {
            val android: BaseExtension by extensions
            val androidComponents: AndroidComponentsExtension<*, *, *> by extensions

            androidComponents.onVariants { variant ->
                val sources = sourceSets.maybeCreate(variant.name)
                val ss = android.sourceSets.getByName(variant.name)

                sources.srcDir(provider { ss.java.srcDirs })
                sources.srcDir(provider { (ss.kotlin as AndroidSourceDirectorySet).srcDirs })
                ss.resources.srcDir(sources)

                afterEvaluate {
                    tasks.named("generate${variant.name.capitalized()}Resources") {
                        it.dependsOn(sources)
                    }
                }
            }
        }

    }

    private class CodeOwnersSourceSet(
        val sources: SourceDirectorySet,
        val generateTask: Provider<CodeOwnersTask>,
    ) : SourceDirectorySet by sources

}
