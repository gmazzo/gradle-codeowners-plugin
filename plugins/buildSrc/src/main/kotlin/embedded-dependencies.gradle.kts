import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    java
}

val embedded by configurations.creating

configurations.compileOnly { extendsFrom(embedded) }
configurations.testRuntimeOnly { extendsFrom(embedded) }

tasks.jar {
    val archiveOperations: ArchiveOperations = gradle.serviceOf()

    from(Callable {
        embedded.map { dependency ->
            when (dependency.extension.lowercase()) {
                "jar" -> archiveOperations.zipTree(dependency).matching {
                    exclude(
                        "META-INF/MANIFEST.MF",
                        "META-INF/*.SF",
                        "META-INF/*.DSA",
                        "META-INF/*.RSA",
                        "META-INF/versions/"
                    )
                }

                else -> dependency
            }
        }
    })
    duplicatesStrategy = DuplicatesStrategy.WARN
}
