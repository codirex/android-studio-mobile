plugins {
    id("asm.android.library")
    // Needed in every module that contains an @AndroidEntryPoint class (ADR-0009).
    alias(libs.plugins.hilt)
}

android {
    namespace = "org.codirex.asm.feature.filetree"
}

dependencies {
    // FileTreeNavigator mentions ProjectName, so the host app sees the domain types.
    api(project(":domain:project"))
    api(project(":domain:filetree"))

    implementation(project(":core:concurrency"))
    implementation(project(":core:ui"))

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    testImplementation(libs.junit4)
}
