plugins {
    kotlin("jvm") version "1.7.21" apply false
}

group = "com.github.gmazzo.codeowners"
version = "1.0-SNAPSHOT"

allprojects {

    repositories {
        mavenCentral()
    }

    plugins.withId("kotlin") {

        dependencies {
            "testImplementation"(kotlin("test"))
            "testImplementation"(platform("org.junit:junit-bom:5.9.1"))
            "testImplementation"("org.junit.jupiter:junit-jupiter")
        }

        tasks.named<Test>("test") {
            useJUnitPlatform()
        }

    }

}
