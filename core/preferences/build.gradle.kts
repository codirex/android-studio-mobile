plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.core.preferences"
}

dependencies {
    // PreferenceStore.observe returns LiveData.
    api(libs.androidx.lifecycle.livedata)

    // Gives us the default shared preferences file and, later, the settings screens.
    implementation(libs.androidx.preference)

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    testImplementation(libs.junit4)
}
