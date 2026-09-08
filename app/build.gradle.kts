import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.vahi.app"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.vahi.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true // exposes versionName/versionCode to code — Settings "About" (VAHI-406)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// Room schema export (room.schemaLocation) is deliberately OFF for now. Room
// 2.8.4's bundled schema-bundle classes (FieldBundle$$serializer etc.) throw
// java.lang.AbstractMethodError ("typeParametersSerializers()") the moment KSP
// re-reads an already-exported schema JSON to diff against — reproduced
// deterministically with a fresh Kotlin daemon and with kotlinx-serialization-core
// forced to both 1.7.3 and 1.8.1, so this is a genuine upstream incompatibility in
// this Room/kotlinx.serialization pairing, not a resolvable version clash on this
// project's end. It only bites on a full `clean` rebuild with a same-version
// schema file already present — normal incremental builds are unaffected.
// Zero functional cost right now: schema is version 1 with no migrations to
// validate against. Revisit (docs/17-data-spec.md §6, R4) when a real schema v2
// exists — either Room will have a fix by then, or re-litigate the Room version.

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Persistence — VAHI-301 scope: notebook metadata only. Stroke/ink geometry is
    // deliberately NOT persisted yet; see docs/17-data-spec.md R1 and the note on
    // CanvasScreen's notebookId parameter.
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Dependency injection — VAHI-202.
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Explicit — BaseViewModel imports androidx.lifecycle.ViewModel and
    // kotlinx.coroutines.flow.*, and previously compiled only because these
    // arrived transitively via activity-compose / lifecycle-runtime-ktx.
    implementation(libs.kotlinx.coroutines.android)

    // Android 12+ SplashScreen API, backported to minSdk via the compat library —
    // replaces the ExoPlayer video splash (D11): no forced wait, no media3.
    implementation(libs.androidx.core.splashscreen)

    // App-wide settings (VAHI-307) — see DataStorePreferencesRepository's KDoc for
    // why this is the plain preferences flavour, not Proto DataStore.
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Stroke's geometry is backed by androidx.compose.ui.graphics.Path, which on
    // Android delegates to android.graphics.Path — a real Android framework type
    // that plain JVM unit tests can't construct. Robolectric shadows it so
    // CanvasState (docs/19-quality-bar.md's highest-value test target) is testable
    // without an emulator.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    // ApplicationProvider.getApplicationContext() for Room's in-memory database
    // builder under Robolectric.
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}