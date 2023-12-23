package io.github.gmazzo.codeowners

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainer

class KotlinSupport(private val project: Project) {

    val kotlin: KotlinProjectExtension by project.extensions

    fun configureTargets(action: KotlinTarget.() -> Unit) {
        project.plugins.withType<KotlinBasePlugin> {
            when (val kotlin = kotlin) {
                is KotlinSingleTargetExtension<*> -> action(kotlin.target)
                is KotlinTargetsContainer -> kotlin.targets.configureEach(action::invoke)
            }
        }
    }

}