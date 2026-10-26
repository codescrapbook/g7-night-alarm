plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("jacoco")
}

android {
    namespace = "com.g7nightalarm.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.g7nightalarm.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Enable multidex if needed in future
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Compose compiler plugin managed by kotlin plugin for Kotlin 2.0+
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xcontext-receivers"
        )
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Default resource processing
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
        animationsDisabled = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        // Clear app data between tests when using orchestrator
        emulatorSnapshots.enableForTestFailures = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("com.google.android.material:material:1.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Room
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Networking for xDrip local web server fallback
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

    // Permissions helpers
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // Offline keyword spotting (Vosk)
    implementation("com.alphacephei:vosk-android:0.3.75")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.12")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    // Robolectric & test utils
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    // Roborazzi for JVM screenshots
    val roborazziVersion = "1.17.0"
    testImplementation("io.github.takahirom.roborazzi:roborazzi:$roborazziVersion")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:$roborazziVersion")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:$roborazziVersion")
    // Instrumented testing
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestUtil("androidx.test:orchestrator:1.5.1")
}

// Allow references to generated code
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// JaCoCo coverage for unit tests
jacoco {
    toolVersion = "0.8.11"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*_Impl*.*",
        "**/androidx/**",
        "**/kotlin/**",
        "**/databinding/**",
        "**/schemas/**"
    )
    val debugTree = fileTree("$buildDir/tmp/kotlin-classes/debug") { exclude(fileFilter) }
    val mainSrc = files("$projectDir/src/main/java")
    classDirectories.setFrom(debugTree)
    sourceDirectories.setFrom(mainSrc)
    // Only include the unit test coverage data to satisfy Gradle 8.9 work validation
    executionData.setFrom(files("$buildDir/jacoco/testDebugUnitTest.exec"))
}

// Exclude heavy Robolectric screenshot tests from release unit test variant
tasks.withType<Test>().configureEach {
    if (name == "testReleaseUnitTest") {
        filter {
            excludeTestsMatching("com.g7nightalarm.app.screenshots.RoborazziScreenshotsTest")
        }
    }
}

