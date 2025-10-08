plugins {
    id("plugin-convention-module")
    id("com.github.gmazzo.buildconfig")
    `java-test-fixtures`
}

description = "Computes the codeowners of the project's classes"

buildConfig {
    packageName = "io.github.gmazzo.codeowners"

    buildConfigField("PLUGIN_VERSION", provider { project.version.toString() })
    buildConfigField("PLUGIN_URL", "https://github.com/gmazzo/gradle-codeowners-plugin")
}

dependencies {
    fun plugin(dep: Provider<PluginDependency>) = with(dep.get()) {
        create("$pluginId:$pluginId.gradle.plugin:$version")
    }

    compileOnly(gradleKotlinDsl())
    compileOnly(plugin(libs.plugins.android.application))
    compileOnly(plugin(libs.plugins.kotlin.jvm))

    implementation(projects.matcher)
    implementation(libs.apache.bcel)

    testFixturesImplementation(gradleTestKit())
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.params)
}

gradlePlugin {
    plugins.create("codeOwners") {
        id = "io.github.gmazzo.codeowners"
        displayName = name
        implementationClass = "io.github.gmazzo.codeowners.CodeOwnersPlugin"
        description = project.description
        tags.addAll("codeowners", "ownership", "attribution")
    }
}
