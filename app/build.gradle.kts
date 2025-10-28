import java.io.FileInputStream
import java.util.Properties
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.jetbrainsKotlinAndroid)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.sonar)
  id("jacoco")
  id("com.google.gms.google-services")
  // Must match the Kotlin compiler version for @Serializable
  id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10"
}

android {
  namespace = "ch.onepass.onepass"
  compileSdk = 34

  val localProperties = Properties()
  val localPropertiesFile = rootProject.file("local.properties")
  if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
  }

  // Load Mapbox token from local.properties (fallback to empty string)
  val mapboxToken: String? = localProperties.getProperty("MAPBOX_ACCESS_TOKEN")
  if (mapboxToken.isNullOrBlank()) {
    logger.warn(
      "⚠️ Mapbox access token not found in local.properties. " +
              "Maps may not function correctly until MAPBOX_ACCESS_TOKEN is set."
    )
  }

  defaultConfig {
    applicationId = "ch.onepass.onepass"
    minSdk = 28
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables { useSupportLibrary = true }

    buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"${mapboxToken ?: ""}\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
    debug {
      enableUnitTestCoverage = true
      enableAndroidTestCoverage = true
    }
  }

  testCoverage { jacocoVersion = "0.8.11" }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  composeOptions {
    // Compatible with Kotlin 1.8.10
    kotlinCompilerExtensionVersion = "1.4.2"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlinOptions { jvmTarget = "11" }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      merges += "META-INF/LICENSE.md"
      merges += "META-INF/LICENSE-notice.md"
      excludes += "META-INF/DEPENDENCIES"
    }
    jniLibs { useLegacyPackaging = true }
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }

  // Robolectric should only run in debug. Move shared tests to testDebug.
  sourceSets.getByName("testDebug") {
    val test = sourceSets.getByName("test")
    java.setSrcDirs(test.java.srcDirs)
    res.setSrcDirs(test.res.srcDirs)
    resources.setSrcDirs(test.resources.srcDirs)
  }
  sourceSets.getByName("test") {
    java.setSrcDirs(emptyList<File>())
    res.setSrcDirs(emptyList<File>())
    resources.setSrcDirs(emptyList<File>())
  }

  signingConfigs {
    create("release") {
      storeFile = file((project.findProperty("RELEASE_STORE_FILE") as String?) ?: "keystore.jks")
      storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
      keyAlias      = project.findProperty("RELEASE_KEY_ALIAS") as String?
      keyPassword   = project.findProperty("RELEASE_KEY_PASSWORD") as String?
      storeType     = (project.findProperty("RELEASE_STORE_TYPE") as String?) ?: "pkcs12"
    }
  }

  buildTypes {
    getByName("release") {
      signingConfig = signingConfigs.getByName("release")
    }
  }
}

sonar {
  properties {
    property("sonar.projectKey", "onepass-ch_onepass")
    property("sonar.projectName", "onepass")
    property("sonar.organization", "onepass-ch")
    property("sonar.host.url", "https://sonarcloud.io")
    property(
      "sonar.junit.reportPaths",
      "${project.layout.buildDirectory.get()}/test-results/testDebugUnitTest/"
    )
    property(
      "sonar.androidLint.reportPaths",
      "${project.layout.buildDirectory.get()}/reports/lint-results-debug.xml"
    )
    property(
      "sonar.coverage.jacoco.xmlReportPaths",
      "${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
    )
  }
}

// When a library is used both by Robolectric and connected tests, use this function
fun DependencyHandlerScope.globalTestImplementation(dep: Any) {
  androidTestImplementation(dep)
  testImplementation(dep)
}

dependencies {
  // Core Android / Kotlin
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation("androidx.datastore:datastore-preferences:1.1.1")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

  // Navigation
  implementation(libs.androidx.navigation.compose)
  implementation("androidx.navigation:navigation-compose:2.6.0")

  // Firebase
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth.ktx)
  implementation(libs.firebase.firestore.ktx)
  implementation(libs.firebase.database.ktx)

  // Jetpack Compose
  val composeBom = platform(libs.compose.bom)
  implementation(composeBom)
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.graphics)
  implementation(libs.compose.material3)
  implementation(libs.compose.activity)
  implementation(libs.compose.viewmodel)
  implementation(libs.compose.preview)
  debugImplementation(libs.compose.tooling)
  debugImplementation(libs.compose.test.manifest)
  globalTestImplementation(composeBom)
  globalTestImplementation(libs.compose.test.junit)

  // Kaspresso test framework
  globalTestImplementation(libs.kaspresso)
  globalTestImplementation(libs.kaspresso.compose)

  // Testing
  testImplementation(libs.junit)
  globalTestImplementation(libs.androidx.junit)
  globalTestImplementation(libs.androidx.espresso.core)
  testImplementation(libs.robolectric)

  // Coroutines test
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
  androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

  // MockK
  testImplementation("io.mockk:mockk:1.13.10")
  androidTestImplementation("io.mockk:mockk-android:1.13.10")

  // Networking / Data
  implementation(libs.okhttp)
  implementation(libs.geofirestore.android)

  // Map / Location
  implementation("com.mapbox.maps:android-ndk27:11.15.2")
  implementation("com.mapbox.extension:maps-compose-ndk27:11.15.2")

  // QR / Google Identity
  implementation("com.google.zxing:core:3.5.1")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
}

tasks.withType<Test> {
  if (name.contains("Release")) {
    exclude("**/*ComposeTest.class")
  }
  configure<JacocoTaskExtension> {
    isIncludeNoLocationClasses = true
    excludes = listOf("jdk.internal.*")
  }
}

tasks.register("jacocoTestReport", JacocoReport::class) {
  mustRunAfter("testDebugUnitTest", "connectedDebugAndroidTest")
  reports {
    xml.required = true
    html.required = true
  }

  val fileFilter = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
  )

  val debugTree =
    fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
      exclude(fileFilter)
    }

  val mainSrc = "${project.layout.projectDirectory}/src/main/java"
  sourceDirectories.setFrom(files(mainSrc))
  classDirectories.setFrom(files(debugTree))
  executionData.setFrom(
    fileTree(project.layout.buildDirectory.get()) {
      include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
      include("outputs/code_coverage/debugAndroidTest/connected/*/coverage.ec")
    }
  )
}

// Global exclusion to avoid protobuf conflicts
configurations.forEach { configuration ->
  configuration.exclude("com.google.protobuf", "protobuf-lite")
}

