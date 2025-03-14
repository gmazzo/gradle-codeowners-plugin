import io.github.gmazzo.codeowners.CodeOwnersReportTask
import io.github.gmazzo.codeowners.CodeOwnersResourcesTask

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

val collectTask = tasks.register<Sync>("collectTaskOutputs") {
    into(temporaryDir)
}

rootProject.allprojects project@{
    tasks.withType<CodeOwnersResourcesTask>().all task@{
        collectTask.configure {
            from(listOf(simplifiedMappedCodeOwnersFile, rawMappedCodeOwnersFile)) {
                into("actualMappings/${this@task.project.path}")
            }
        }
    }
    tasks.withType<CodeOwnersReportTask>().all task@{
        collectTask.configure {
            from(codeOwnersReportFile) {
                into("actualReports/${this@task.project.path}")
            }
        }
    }
}

tasks.register<Sync>("updateTestSpecs") {
    from(collectTask.map { it.destinationDir.resolve("actualMappings") }) { into("expectedMappings") }
    from(collectTask.map { it.destinationDir.resolve("actualReports") }) { into("expectedReports") }
    into(layout.projectDirectory.dir("src/test/resources"))
}

sourceSets.test {
    resources.srcDirs(collectTask)
}

tasks.check {
    rootProject.allprojects {
        dependsOn(tasks.withType<CodeOwnersReportTask>())
    }
}
