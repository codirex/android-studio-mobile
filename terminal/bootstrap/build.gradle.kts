plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.terminal.bootstrap"
}

dependencies {
    implementation(project(":core:fs"))

    testImplementation(libs.junit4)
}
