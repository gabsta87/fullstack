import org.gradle.kotlin.dsl.maven

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
//        maven { url = uri("https://repo.spring.io/release") }
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
}

rootProject.name = "serv"