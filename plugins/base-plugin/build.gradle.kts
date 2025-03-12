plugins {
    id("plugin-convention-module")
    `java-test-fixtures`
}

description = "Computes the codeowners of the project's classes"

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
    testFixturesApi("org.junit.jupiter:junit-jupiter-params")
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
