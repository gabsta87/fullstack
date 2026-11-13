import org.gradle.kotlin.dsl.maven

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://repo.spring.io/release") }
    }
}

rootProject.name = "serv"