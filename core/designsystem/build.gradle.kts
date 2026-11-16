plugins {
    id("photidy.android.compose")
}

android {
    namespace = "app.photidy.core.designsystem"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:localization"))
    implementation(project(":core:platform"))

    implementation(libs.coil.compose)
}
