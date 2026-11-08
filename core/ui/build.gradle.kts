plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.core.ui"
}

dependencies {
    // Material types appear in every screen's code, so features get them through this module.
    api(libs.material)
}
