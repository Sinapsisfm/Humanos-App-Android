plugins {
    id("humanos.android.library")
    id("humanos.hilt")
    alias(libs.plugins.room)
}

android {
    namespace = "eco.humanos.android.core.database"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:core-model"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
}
