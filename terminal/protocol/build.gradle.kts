plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.terminal.protocol"

    buildFeatures {
        aidl = true
    }
}
