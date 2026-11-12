plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.terminal.view"
}

dependencies {
    implementation(project(":terminal:emulator"))
    // Its types (ITerminalService) are in the signatures of TerminalView and TerminalConnection.
    api(project(":terminal:protocol"))
    implementation(project(":terminal:service"))

    testImplementation(libs.junit4)
}
