import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "eco.humanos.android.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "humanos.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "humanos.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "humanos.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("compose") {
            id = "humanos.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("hilt") {
            id = "humanos.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "humanos.kotlin.library"
            implementationClass = "KotlinConventionPlugin"
        }
    }
}
