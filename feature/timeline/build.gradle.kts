plugins {
    id("photidy.android.compose")
}

android {
    namespace = "app.photidy.feature.timeline"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:localization"))
}
