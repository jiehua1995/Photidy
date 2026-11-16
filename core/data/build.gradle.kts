plugins {
    id("photidy.android.library")
}

android {
    namespace = "app.photidy.core.data"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:platform"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
}
