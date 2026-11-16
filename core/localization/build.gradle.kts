plugins {
    id("photidy.android.library")
}

android {
    namespace = "app.photidy.core.localization"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
}
