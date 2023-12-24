import buildSrc.BuildConfig

plugins {
    `java-gradle-plugin`
}

val compatibilityTest by testing.suites.registering(JvmTestSuite::class) {
    dependencies {
        implementation(testFixtures(project(":base-plugin")))
    }
    targets.configureEach {
        testTask.configure {
            javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(17) }
            workingDir(temporaryDir)
        }
    }
}

val compatibilityTestSelfPlugin by configurations.creating
val compatibilityTestKotlinPlugin by configurations.creating
val compatibilityTestAndroidPlugin by configurations.creating

dependencies {
    compatibilityTestSelfPlugin(project)
    compatibilityTestKotlinPlugin(BuildConfig.KOTLIN_PLUGIN)
    compatibilityTestAndroidPlugin(BuildConfig.ANDROID_PLUGIN)
}

val collectPluginMetadata by tasks.registering(CollectDependencies::class) {
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
