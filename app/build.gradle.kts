import java.util.Properties

plugins { id("com.android.application"); kotlin("android"); kotlin("plugin.compose"); kotlin("plugin.serialization") }
val integration = Properties().apply { rootProject.file("integration.properties").takeIf { it.isFile }?.inputStream()?.use(::load) }
fun setting(name: String) = providers.gradleProperty(name).orNull ?: integration.getProperty(name, "")
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""
// Preserve the legacy APK signing contract. These values never affect ETS100 API signing.
val releaseSigningInputs = listOf("ETS_SIGNING_STORE", "ETS_SIGNING_STORE_PASSWORD", "ETS_SIGNING_ALIAS", "ETS_SIGNING_KEY_PASSWORD")
    .associateWith { providers.environmentVariable(it).orNull?.takeIf(String::isNotBlank) }
val releaseKeyStore = releaseSigningInputs["ETS_SIGNING_STORE"]?.let { file(it) }
val releaseSigningRequested = releaseSigningInputs.values.any { it != null }
val releaseSigningComplete = releaseSigningInputs.values.all { it != null } && releaseKeyStore?.isFile == true
android {
    namespace = "com.etsviewer.android"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.etsviewer.android"
        minSdk = 26
        targetSdk = 35
        versionCode = providers.gradleProperty("versionCode").orElse("3").get().toInt().also {
            require(it >= 3) { "versionCode must be at least 3, above the verified legacy version 2" }
        }
        versionName = providers.gradleProperty("versionName").orElse("1.0.0").get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "ETS_PID", quoted(setting("ets.pid")))
        buildConfigField("String", "ETS_SIGNING_SECRET", quoted(setting("ets.signingSecret")))
        buildConfigField("String", "ETS_SERIAL", quoted(setting("ets.serial").ifBlank { "test" }))
    }
    signingConfigs {
        if (releaseSigningComplete) create("release") {
            storeFile = releaseKeyStore
            storePassword = releaseSigningInputs.getValue("ETS_SIGNING_STORE_PASSWORD")
            keyAlias = releaseSigningInputs.getValue("ETS_SIGNING_ALIAS")
            keyPassword = releaseSigningInputs.getValue("ETS_SIGNING_KEY_PASSWORD")
        }
    }
    buildTypes {
        release { if (releaseSigningComplete) signingConfig = signingConfigs.getByName("release") }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
val validateReleaseSigningInputs by tasks.registering {
    doLast {
        check(!releaseSigningRequested || releaseSigningComplete) {
            "Original Release signing configuration is incomplete or its keystore is unavailable; check ETS_SIGNING_* inputs privately"
        }
        if (!releaseSigningComplete) logger.lifecycle("Original release signing key unavailable — compatible in-place upgrade cannot be verified. Building unsigned Release APK.")
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validateReleaseSigningInputs) }
// The app's requested unit-test entry point includes the pure Kotlin production engine.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach { dependsOn(":engine:test") }
dependencies {
    implementation(project(":engine"))
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.webkit:webkit:1.12.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.squareup.okhttp3:okhttp-tls:4.12.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("com.squareup.okhttp3:okhttp-tls:4.12.0")
}
