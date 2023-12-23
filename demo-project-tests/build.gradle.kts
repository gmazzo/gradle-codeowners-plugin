import io.github.gmazzo.codeowners.CodeOwnersReportTask
import io.github.gmazzo.codeowners.CodeOwnersTask

// This should not be considered part of the demo project
// It was added to be able to create test tasks for the generated mapping files

plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.kotlin.test)
}

testing.suites.withType<JvmTestSuite>().configureEach {
    useJUnitJupiter()
}

val collectSources = copySpec()
val collectTask = tasks.register<Sync>("collectMappings") {
    with(collectSources)
    into(temporaryDir)
}

rootProject.allprojects project@{
    tasks.withType<CodeOwnersTask>().all task@{
        collectSources.from(files(simplifiedMappedCodeOwnersFile, rawMappedCodeOwnersFile)) {
            into("actualMappings/${project.path}")
        }
    }
}

sourceSets.test {
    resources.srcDirs(collectTask)
}

tasks.check {
    rootProject.allprojects {
        dependsOn(tasks.withType<CodeOwnersReportTask>())
    }
}