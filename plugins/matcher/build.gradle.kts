plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.importClasses)
    `jvm-convention-module`
}

description = "CodeOwners Matcher Library"

sourceSets.main {
    importClasses(libs.jgit) {
        repackageTo = "io.github.gmazzo.codeowners.matcher.jgit"
        keep("org.eclipse.jgit.ignore.FastIgnoreRule")
        include("**.class")
    }
}

dependencies {
    implementation(libs.slf4j) // transitive of libs.jgit, required by FastIgnoreRule
    testImplementation(libs.junit.params)
}
