plugins {
    id("humanos.android.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "eco.humanos.android.data.maps"
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
    // Dominio de mapas (contrato MapRepository + modelos). Sin proveedor/red.
    implementation(project(":core:core-maps"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Unit tests JVM (mappers puros).
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    // Robolectric: ejecuta Room (DAO/restore) en JVM sin emulador.
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation(libs.androidx.test.core)

    // Instrumented (PREPARADO, no requerido: el adapter está DESACTIVADO/no cableado).
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.truth)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.room:room-testing:2.7.1")
}
