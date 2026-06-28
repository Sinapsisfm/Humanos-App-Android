plugins {
    id("humanos.android.library")
    id("humanos.hilt")
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "eco.humanos.android.data.outdoor"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Dominio Outdoor (contrato OutdoorRepository + entidades de dominio).
    implementation(project(":core:core-outdoor"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Unit tests JVM (mappers puros).
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    // Robolectric: ejecuta Room (DAO/restore) en JVM sin emulador (evidencia Android real).
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation(libs.androidx.test.core)

    // Instrumented (PREPARADO, no ejecutado: requiere emulador/dispositivo).
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.truth)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.room:room-testing:2.7.1")
}
