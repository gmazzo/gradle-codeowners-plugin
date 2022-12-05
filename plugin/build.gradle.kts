plugins {
    alias(libs.plugins.kotlin)
    `java-gradle-plugin`
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation(projects.core)
    implementation(projects.parser)
}

gradlePlugin.plugins.create("codeOwners") {
    id = "com.github.gmazzo.codeowners"
    implementationClass = "com.github.gmazzo.codeowners.CodeOwnersPlugin"
}
