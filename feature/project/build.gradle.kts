plugins {
    id("asm.android.library")
    // Needed in every module that contains an @AndroidEntryPoint class (ADR-0009).
    alias(libs.plugins.hilt)
}

android {
    namespace = "org.codirex.asm.feature.project"
}

dependencies {
    // ProjectNavigator mentions ProjectName, so the host app sees the domain types.
    api(project(":domain:project"))
    // The create screen uses the ProjectCreator use case; Hilt-generated code in the app mentions it.
    api(project(":domain:template"))

    implementation(project(":core:concurrency"))
    implementation(project(":core:ui"))

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    testImplementation(libs.junit4)
}
