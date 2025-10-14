import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.sonar)
    id("jacoco")
    id("com.google.gms.google-services")
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
        vectorDrawables {
            useSupportLibrary = true
        }
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

    testCoverage {
        jacocoVersion = "0.8.11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/DEPENDENCIES"
        }
        packagingOptions {
            jniLibs {
                useLegacyPackaging = true
            }
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
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
}

    sonar {
        properties {
            property("sonar.projectKey", "onepass-ch_onepass")
            property("sonar.projectName", "onepass")
            property("sonar.organization", "onepass-ch")
            property("sonar.host.url", "https://sonarcloud.io")
            // Comma-separated paths to the various directories containing the *.xml JUnit report files. Each path may be absolute or relative to the project base directory.
            property("sonar.junit.reportPaths", "${project.layout.buildDirectory.get()}/test-results/testDebugunitTest/")
            // Paths to xml files with Android Lint issues. If the main flavor is changed, this file will have to be changed too.
            property("sonar.androidLint.reportPaths", "${project.layout.buildDirectory.get()}/reports/lint-results-debug.xml")
            // Paths to JaCoCo XML coverage report files.
            property("sonar.coverage.jacoco.xmlReportPaths", "${project.layout.buildDirectory.get()}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        }
    }

    // When a library is used both by robolectric and connected tests, use this function
    fun DependencyHandlerScope.globalTestImplementation(dep: Any) {
        androidTestImplementation(dep)
        testImplementation(dep)
    }

    dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(platform(libs.compose.bom))
        testImplementation(libs.junit)
        globalTestImplementation(libs.androidx.junit)
        globalTestImplementation(libs.androidx.espresso.core)

        // ------------- Jetpack Compose ------------------
        val composeBom = platform(libs.compose.bom)
        implementation(composeBom)
        globalTestImplementation(composeBom)

        implementation(libs.compose.ui)
        implementation(libs.compose.ui.graphics)
        // Material Design 3
        implementation(libs.compose.material3)
        // Integration with activities
        implementation(libs.compose.activity)
        // Integration with ViewModels
        implementation(libs.compose.viewmodel)
        // Android Studio Preview support
        implementation(libs.compose.preview)
        debugImplementation(libs.compose.tooling)
        // UI Tests
        globalTestImplementation(libs.compose.test.junit)
        debugImplementation(libs.compose.test.manifest)

        // --------- Kaspresso test framework ----------
        globalTestImplementation(libs.kaspresso)
        globalTestImplementation(libs.kaspresso.compose)

        // ----------       Robolectric     ------------
        testImplementation(libs.robolectric)

        // ---------- ZXing for QR code generation ------------
        implementation("com.google.zxing:core:3.5.1")

        // ----------       MapBox         ------------
        implementation("com.mapbox.maps:android-ndk27:11.15.2")
        implementation("com.mapbox.extension:maps-compose-ndk27:11.15.2")

        // Firebase auth
      implementation ("androidx.credentials:credentials:1.2.0")
      implementation ("androidx.credentials:credentials-play-services-auth:1.2.0")
      implementation (platform("com.google.firebase:firebase-bom:33.3.0"))
      implementation ("com.google.firebase:firebase-auth")
      implementation ("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    }

    tasks.withType<Test> {
        // Exclude Compose UI JVM tests from release unit tests (require debug-only test manifest)
        if (name.contains("Release")) {
            exclude("**/*ComposeTest.class")
        }

        // Configure Jacoco for each tests
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