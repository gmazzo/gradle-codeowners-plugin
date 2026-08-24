plugins {
    `java-gradle-plugin`
}

val compatibilityTest = testing.suites.register<JvmTestSuite>("compatibilityTest") {
    dependencies {
        implementation(testFixtures(project(":base-plugin")))
    }
    targets.configureEach {
        testTask.configure {
            workingDir(temporaryDir)
        }
    }
}

fun prepare(config: Configuration) = with(config) {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.CLASSES_AND_RESOURCES))
    }
}

val compatibilityTestSelfPlugin = configurations.create("compatibilityTestSelfPlugin", ::prepare)
val compatibilityTestKotlinPlugin = configurations.create("compatibilityTestKotlinPlugin", ::prepare)
val compatibilityTestAndroidPlugin = configurations.create("compatibilityTestAndroidPlugin", ::prepare)

dependencies {
    compatibilityTestSelfPlugin(project)
    compatibilityTestKotlinPlugin(BuildConfig.KOTLIN_PLUGIN)
    compatibilityTestAndroidPlugin(BuildConfig.ANDROID_PLUGIN)
}

val collectPluginMetadata = tasks.register<CollectDependencies>("collectPluginMetadata") {
    aloneClasspath.from(compatibilityTestSelfPlugin)
    kotlinClasspath.from(compatibilityTestSelfPlugin, compatibilityTestKotlinPlugin)
    androidClasspath.from(compatibilityTestSelfPlugin, compatibilityTestAndroidPlugin)
    bothClasspath.from(compatibilityTestSelfPlugin, compatibilityTestKotlinPlugin, compatibilityTestAndroidPlugin)
    outputDirectory.set(temporaryDir)
}

sourceSets.named(::compatibilityTest.name) {
    resources.srcDirs(collectPluginMetadata)
}

tasks.check {
    dependsOn(compatibilityTest)
}

@CacheableTask
abstract class CollectDependencies : DefaultTask() {

    @get:Classpath
    abstract val aloneClasspath: ConfigurableFileCollection

    @get:Classpath
    abstract val kotlinClasspath: ConfigurableFileCollection

    @get:Classpath
    abstract val androidClasspath: ConfigurableFileCollection

    @get:Classpath
    abstract val bothClasspath: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generateResources() {
        val outDir = outputDirectory.get().asFile
        outDir.deleteRecursively()
        outDir.mkdirs()

        File(outDir, "alone.classpath").writeText(aloneClasspath.asPath)
        File(outDir, "withKotlin.classpath").writeText(kotlinClasspath.asPath)
        File(outDir, "withAndroid.classpath").writeText(androidClasspath.asPath)
        File(outDir, "withBoth.classpath").writeText(bothClasspath.asPath)
    }

}
