plugins {
    id("asm.android.application")
    alias(libs.plugins.hilt)
    // Adds the Codirex Trace SDK to debug builds: logs stream live to the Trace viewer app.
    id("org.codirex.trace") version "1.0.0"
}

android {
    namespace = "org.codirex.asm"

    defaultConfig {
        applicationId = "org.codirex.asm"
        // CI builds count up, so a build can always replace the one before it.
        versionCode = providers.environmentVariable("GITHUB_RUN_NUMBER").map { it.toInt() }.orElse(1).get()
        versionName = "0.0.1"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:concurrency"))
    implementation(project(":core:preferences"))
    implementation(project(":core:ui"))

    // Hilt generates code in the app that mentions the domain ports, so their modules must be visible here.
    implementation(project(":domain:editor"))
    implementation(project(":domain:filetree"))
    implementation(project(":domain:template"))
    implementation(project(":domain:environment"))
    implementation(project(":domain:toolchain"))
    implementation(project(":domain:sdk"))

    // The app wires data modules to the domain ports through Hilt; features never see them.
    implementation(project(":data:project"))
    implementation(project(":data:template"))
    implementation(project(":data:environment"))
    implementation(project(":data:toolchain"))
    implementation(project(":data:sdk"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:filetree"))
    implementation(project(":feature:project"))
    implementation(project(":feature:terminal"))
    implementation(project(":feature:environment"))
    implementation(project(":feature:toolchain"))
    implementation(project(":feature:sdk"))

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)
}
