plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room3)
}

android {
    namespace = "app.palm"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "app.palm.dev" // D-08: pre-release id, replace with the brand id before the first Play upload
        minSdk = 26
        targetSdk = 36 // explicit: AGP 9 would default it to compileSdk
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "BASE_URL", "\"http://127.0.0.1:8000\"")
        }
        release {
            signingConfig = signingConfigs.getByName("debug") // shortcut: debug-signed only for on-device timing (D-16), replace with the upload key at REL-03
            buildConfigField("String", "BASE_URL", "\"https://api.invalid\"")
            ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") } // release ABIs; debug keeps all so the x86_64 emulator runs
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    androidResources {
        // Ship only our two languages; drops library translations (AGP 8.8+ replacement for resourceConfigurations)
        localeFilters += listOf("en", "hi")
        noCompress += "task" // MediaPipe maps the model straight from the APK, so it must be stored (D-06)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        // JVM tests decode the shared contract samples (FND-05)
        unitTests.all { it.systemProperty("contracts.dir", rootProject.file("../contracts").absolutePath) }
        unitTests.all { it.systemProperty("fixtures.dir", file("src/main/assets/fixtures").absolutePath) }
    }
}

room3 { schemaDirectory("$projectDir/schemas") }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mediapipe.tasks.vision)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.concurrent.futures.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.heifwriter) // only PhotoProcessorTest#heicImport (D-16)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
