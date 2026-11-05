import java.util.Properties
import java.util.concurrent.TimeUnit

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun signingValue(property: String, environment: String): String? =
    (keystoreProperties.getProperty(property) ?: System.getenv(environment))
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

val releaseStoreFile = signingValue("storeFile", "RELEASE_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "RELEASE_KEY_PASSWORD")

val releaseSigningValues = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
)
val hasReleaseSigning = releaseSigningValues.all { it != null }

data class ReleaseVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    override fun toString(): String = "$major.$minor.$patch"
}

fun parseReleaseVersion(value: String): ReleaseVersion? =
    Regex("""^v?(\d+)\.(\d+)\.(\d+)$""")
        .matchEntire(value.trim())
        ?.let { match ->
            val (major, minor, patch) = match.destructured
            ReleaseVersion(major.toInt(), minor.toInt(), patch.toInt())
        }

fun latestGitHubReleaseVersion(): String? = runCatching {
    val process = ProcessBuilder("git", "ls-remote", "--tags", "--refs", "origin")
        .directory(rootProject.projectDir)
        .redirectErrorStream(true)
        .start()
    if (!process.waitFor(10, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        return@runCatching null
    }
    val output = process.inputStream.bufferedReader().use { it.readText() }
    if (process.exitValue() != 0) return@runCatching null
    output.lineSequence()
        .mapNotNull { line -> parseReleaseVersion(line.substringAfterLast("refs/tags/", "")) }
        .maxOrNull()
        ?.toString()
}.getOrNull()

fun latestLocalReleaseVersion(): String? = runCatching {
    val process = ProcessBuilder("git", "tag", "--list")
        .directory(rootProject.projectDir)
        .redirectErrorStream(true)
        .start()
    if (!process.waitFor(5, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        return@runCatching null
    }
    val output = process.inputStream.bufferedReader().use { it.readText() }
    if (process.exitValue() != 0) return@runCatching null
    output.lineSequence()
        .mapNotNull(::parseReleaseVersion)
        .maxOrNull()
        ?.toString()
}.getOrNull()

fun releaseVersionCode(versionName: String): Int {
    val match = Regex("""^v?(\d+)\.(\d+)\.(\d+)""").find(versionName.trim()) ?: return 1
    val (major, minor, patch) = match.destructured
    return major.toInt() * 1_000_000 + minor.toInt() * 1_000 + patch.toInt()
}

val buildVersionName = System.getenv("VERSION_NAME")?.trim()?.takeIf { it.isNotEmpty() }
    ?: latestGitHubReleaseVersion()
    ?: latestLocalReleaseVersion()
    ?: "1.0.5"
val buildVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: releaseVersionCode(buildVersionName)

if (releaseSigningValues.any { it != null } && !hasReleaseSigning) {
    throw GradleException(
        "Release signing is partially configured. Set all of " +
            "RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, " +
            "RELEASE_KEY_PASSWORD (or all four keys in keystore.properties).",
    )
}

android {
    namespace = "xyz.lisfox.vocaland"
    compileSdk = 36

    defaultConfig {
        applicationId = "xyz.lisfox.vocaland"
        minSdk = 29
        targetSdk = 35
        versionCode = buildVersionCode
        versionName = buildVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val releaseSigningConfig = if (hasReleaseSigning) {
        signingConfigs.create("release") {
            storeFile = rootProject.file(releaseStoreFile!!)
            storePassword = releaseStorePassword!!
            keyAlias = releaseKeyAlias!!
            keyPassword = releaseKeyPassword!!
        }
    } else {
        null
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = releaseSigningConfig
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}