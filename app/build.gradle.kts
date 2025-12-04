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

    // Get Stripe publishable key from local.properties
    val stripePublishableKey: String? = localProperties.getProperty("STRIPE_PUBLISHABLE_KEY")

    if (stripePublishableKey.isNullOrBlank()) {
        logger.warn(
            "⚠️ Stripe publishable key not found in local.properties. " +
                    "Payment features will not function correctly until STRIPE_PUBLISHABLE_KEY is set."
        )
    }

    defaultConfig {
        applicationId = "ch.onepass.onepass"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        testInstrumentationRunnerArguments["disableAnalytics"] = "true"
        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"${mapboxToken}\"")
        buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"${stripePublishableKey ?: ""}\"")
        val oneSignalAppId: String? = localProperties.getProperty("ONESIGNAL_APP_ID")
        buildConfigField("String", "ONESIGNAL_APP_ID", "\"${oneSignalAppId ?: ""}\"")

        // Enable multidex for handling large number of methods (Compose UI, etc.)
        multiDexEnabled = true
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
        jniLibs { useLegacyPackaging = true }
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

    val keystorePath = (project.findProperty("RELEASE_STORE_FILE") as String?) ?: "keystore.jks"
    val keystoreFile = file(keystorePath)

    if (keystoreFile.exists()) {
        signingConfigs {
            create("release") {
                storeFile = keystoreFile
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
    } else {
        logger.warn("⚠️ Keystore file not found at: $keystorePath. Release builds will not be signed.")
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
    // Multidex support for handling large number of methods
    implementation(libs.androidx.multidex)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ML Kit Barcode Scanning
    implementation(libs.mlkit.barcode.scanning)
    // CameraX

    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)


    // ------------- Datastore ------------------
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    testImplementation("androidx.datastore:datastore-preferences:1.1.1")
    androidTestImplementation("androidx.datastore:datastore-preferences:1.1.1")

    // ------------- Firebase ------------------
    implementation(libs.firebase.functions.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.appcheck.ktx)
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

    // ---------- Stripe ------------
    implementation(libs.stripe.android)

    // ---------- Google Sign-In (Credential Manager GoogleID) ------------
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    // ---------- Networking with OkHttp ----------
    implementation(libs.okhttp)

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

    // --------- OneSignal -------
    implementation(libs.onesignal)
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

// Configure coverage reporting to handle our source directory structure
afterEvaluate {
    // Configure the Android test coverage report task
    tasks.findByName("createDebugAndroidTestCoverageReport")?.let { task ->
        task.doFirst {
            logger.info("Running createDebugAndroidTestCoverageReport - source dirs configured")
        }
    }
}

tasks.register("jacocoTestReport", JacocoReport::class) {
    // You *can* keep this dependsOn if you still want tests to run
    // when you invoke this task locally. In CI, tests are run in other jobs,
    // but this won't hurt as long as they still succeed.
    dependsOn("testDebugUnitTest")

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

    // Class files for debug variant (Kotlin)
    val debugTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    // Include both Java and Kotlin source dirs (main + debug)
    val mainSrc = files(
        "${project.layout.projectDirectory}/src/main/java",
        "${project.layout.projectDirectory}/src/main/kotlin",
        "${project.layout.projectDirectory}/src/debug/java",
        "${project.layout.projectDirectory}/src/debug/kotlin",
    )

    sourceDirectories.setFrom(mainSrc)
    classDirectories.setFrom(files(debugTree))

    // Collect execution data from unit tests and connected tests
    // – paths match what CI reconstructs in the sonar job
    val execDataFiles = fileTree(project.layout.buildDirectory.get()) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        include("outputs/code_coverage/debugAndroidTest/connected/**/*.ec")
    }.files.filter { it.exists() }

    executionData.setFrom(execDataFiles)
}

configurations.forEach { configuration ->
  configuration.exclude("com.google.protobuf", "protobuf-lite")
}
