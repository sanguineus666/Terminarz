buildscript {
    dependencies {
        // AGP 9.x uses built-in Kotlin. This raises the Kotlin runtime used by AGP
        // so the Compose compiler plugin can use the same Kotlin generation.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

plugins {
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
