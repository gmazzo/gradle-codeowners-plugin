package io.github.gmazzo.codeowners

import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JVM_RESOURCES_DIRECTORY
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails

internal class CodeOwnersCompatibilityRule : AttributeCompatibilityRule<String> {

    companion object {
        const val ARTIFACT_TYPE_CODEOWNERS = "codeowners"
    }

    override fun execute(details: CompatibilityCheckDetails<String>) = with(details) {
        if (consumerValue == ARTIFACT_TYPE_CODEOWNERS && producerValue == JVM_RESOURCES_DIRECTORY) {
            compatible()
        }
    }

}
