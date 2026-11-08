plugins {
    id("asm.android.library")
}

android {
    namespace = "org.codirex.asm.core.concurrency"
}

dependencies {
    // Clotho types (Realm, Job, Lane) appear in this module's API, so consumers see them.
    // This is the only module allowed to name Clotho in its build file (ADR-0003 rule 4).
    api(libs.clotho.android)

    api(libs.androidx.lifecycle.viewmodel)
    api(libs.androidx.lifecycle.livedata)

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    testImplementation(libs.junit4)
}
