plugins {
	alias(libs.plugins.android.application)
}

android {
	namespace = "org.codirex.asm"
	compileSdk = 36

	defaultConfig {
		applicationId = "org.codirex.asm"
		minSdk = 24
		targetSdk = 28
		versionCode = 1
		versionName = "0.0.1"
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	lint {
		// Distributed through GitHub Releases, not Google Play (ADR-0002).
		disable += setOf("ExpiredTargetSdkVersion", "OldTargetApi")
	}
}

dependencies {
	implementation(project(":core:common"))
}
