import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("photidy.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<LibraryExtension> {
    buildFeatures {
        compose = true
    }
}

dependencies {
    val bom = catalog.findLibrary("compose-bom").get()
    add("implementation", platform(bom))
    add("androidTestImplementation", platform(bom))

    add("implementation", catalog.findLibrary("compose-ui").get())
    add("implementation", catalog.findLibrary("compose-foundation").get())
    add("implementation", catalog.findLibrary("compose-material3").get())
    add("implementation", catalog.findLibrary("compose-material-icons-core").get())
    add("implementation", catalog.findLibrary("compose-ui-tooling-preview").get())
    add("implementation", catalog.findLibrary("androidx-lifecycle-runtime-compose").get())
    add("implementation", catalog.findLibrary("androidx-lifecycle-viewmodel-compose").get())

    add("debugImplementation", catalog.findLibrary("compose-ui-tooling").get())
    add("debugImplementation", catalog.findLibrary("compose-ui-test-manifest").get())

    add("androidTestImplementation", catalog.findLibrary("compose-ui-test-junit4").get())
}
