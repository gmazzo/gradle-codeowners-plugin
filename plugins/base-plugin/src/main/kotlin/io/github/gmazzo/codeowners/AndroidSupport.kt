package io.github.gmazzo.codeowners

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Component
import com.android.build.api.variant.HasAndroidTest
import com.android.build.api.variant.HasUnitTest
import com.android.build.api.variant.Variant
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate

class AndroidSupport(private val project: Project) {

    val androidComponents: AndroidComponentsExtension<*, *, *> by project.extensions

    fun configureVariants(action: Variant.() -> Unit) {
        project.plugins.withId("com.android.base") {
            with(androidComponents) {
                onVariants(selector().all(), action)
            }
        }
    }

    fun configureComponents(action: Component.() -> Unit) = configureVariants {
        action()
        (this as? HasUnitTest)?.unitTest?.action()
        (this as? HasAndroidTest)?.androidTest?.action()
    }

}
