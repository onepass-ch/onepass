import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.jetbrainsKotlinAndroid)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.sonar)
  id("jacoco")
  id("com.google.gms.google-services")
  alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ch.onepass.onepass"
    compileSdk = 34

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(FileInputStream(localPropertiesFile))
    }

    // Get Mapbox token from local.properties
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
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"${mapboxToken}\"")
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
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/DEPENDENCIES"
        }
        packagingOptions {
            jniLibs { useLegacyPackaging = true }
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        animationsDisabled = true
    }

    // Robolectric needs to be run only in debug. But its tests are placed in the shared source set (test)
    // The next lines transfer the src/test/* from shared to the testDebug one
    // This prevents errors from occurring during unit tests
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

// When a library is used both by robolectric and connected tests, use this function
fun DependencyHandlerScope.globalTestImplementation(dep: Any) {
  androidTestImplementation(dep)
  testImplementation(dep)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")


    // ------------- Firebase ------------------
    implementation(libs.firebase.functions.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.database.ktx)
    androidTestImplementation(libs.androidx.navigation.testing)

    // ------------- Jetpack Compose ------------------
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    globalTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.compose.viewmodel)
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)
    globalTestImplementation(libs.compose.test.junit)
    debugImplementation(libs.compose.test.manifest)

    // --------- Kaspresso test framework ----------
    globalTestImplementation(libs.kaspresso)
    globalTestImplementation(libs.kaspresso.compose)

    // ---------- Robolectric ------------
    testImplementation(libs.robolectric)

    // --------- Networking with OkHttp ---------
    implementation(libs.okhttp)

    // ------------- GeoFirestore ------------------
    implementation(libs.geofirestore.android)

    // --------- Coroutines Test Support ---------
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // ---------- ZXing for QR code generation ------------
    implementation("com.google.zxing:core:3.5.1")

    // ---------- libphonenumber for phone number formatting ------------
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.17")

    // ---------- MapBox ------------
    implementation("com.mapbox.maps:android-ndk27:11.15.2")
    implementation("com.mapbox.extension:maps-compose-ndk27:11.15.2")

    // ---------- Navigation --------
    implementation("androidx.navigation:navigation-compose:2.6.0")

    // ---------- Google Sign-In (Credential Manager GoogleID) ------------
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    // --------- MockK for Mocking (unified version) ---------
    testImplementation("io.mockk:mockk:1.13.10")
    androidTestImplementation("io.mockk:mockk-android:1.13.10")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    // --------- JUnit ---------
    testImplementation(libs.junit)
    globalTestImplementation(libs.androidx.junit)
    globalTestImplementation(libs.androidx.espresso.core)
    testImplementation(kotlin("test"))
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

    val debugTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    val mainSrc = "${project.layout.projectDirectory}/src/main/java"
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get()) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        include("outputs/code_coverage/debugAndroidTest/connected/*/coverage.ec")
    })
}

configurations.forEach { configuration ->
  configuration.exclude("com.google.protobuf", "protobuf-lite")
}
