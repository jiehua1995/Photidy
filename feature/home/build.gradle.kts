plugins {
    id("photidy.android.compose")
}

android {
    namespace = "app.photidy.feature.home"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:platform"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:localization"))

    implementation(libs.androidx.activity.compose)
}
