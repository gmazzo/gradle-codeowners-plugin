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
    val prefix = path.removePrefix(":").replace(":", "-")

    tasks.withType<CodeOwnersResourcesTask>().all task@{
        collectTask.configure {
            from(listOf(simplifiedMappedCodeOwnersFile, rawMappedCodeOwnersFile)) {
                into("actualMappings/$prefix")
            }
        }
    }
    tasks.withType<CodeOwnersReportTask>().all task@{
        collectTask.configure {
            from(
                files(
                    reports.mappings.outputLocation,
                    reports.html.outputLocation,
                    reports.xml.outputLocation,
                    reports.checkstyle.outputLocation,
                    reports.sarif.outputLocation,
                )
            ) {
                into("actualReports/$prefix")
                filter {
                    it.replace("(?<=CodeOwners Gradle Plugin ).*?(?=<)".toRegex(), "x.y.z")
                        .replace("(?<=file:).*?(/gradle-codeowners-plugin)?(?=/gradle-codeowners-plugin)".toRegex(), "<baseDir>")
                }
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
