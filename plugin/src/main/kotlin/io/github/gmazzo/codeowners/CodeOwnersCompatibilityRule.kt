package io.github.gmazzo.codeowners

import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JVM_RESOURCES_DIRECTORY
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails

internal class CodeOwnersCompatibilityRule : AttributeCompatibilityRule<String> {

    companion object {
        const val ARTIFACT_TYPE_CODEOWNERS = "codeowners"
    }

    private val javaResArtifacts = setOf(
        JVM_RESOURCES_DIRECTORY,
        "android-java-res" // com.android.build.gradle.internal.publishing.AndroidArtifacts.TYPE_JAVA_RES
    )

    override fun execute(details: CompatibilityCheckDetails<String>) = with(details) {
        if (consumerValue == ARTIFACT_TYPE_CODEOWNERS && producerValue in javaResArtifacts) {
            compatible()
        }
    }

}
