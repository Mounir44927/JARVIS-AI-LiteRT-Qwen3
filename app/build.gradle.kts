plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.jarvis.ai"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jarvis.ai"
        minSdk = 26
        targetSdk = 35
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        versionCode = 1
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "DEFAULT_GEMINI_MODEL", "\"gemini-2.5-flash\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"${project.findProperty("backendBaseUrl") ?: System.getenv("JARVIS_BACKEND_URL") ?: ""}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("META-INF/LICENSE*", "META-INF/NOTICE*")
        resources.pickFirsts.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")

        // Sherpa-ONNX 1.10.35 and OpenWakeWord 0.1.5 both use
        // ONNX Runtime 1.18.0. They therefore provide the same native
        // library path. Pick one copy only after aligning both versions.
        jniLibs.pickFirsts.add("**/libonnxruntime.so")
        // Rive Android ships libc++_shared alongside other native voice runtimes.
        // Keep the existing app copy deterministic when multiple dependencies package it.
        jniLibs.pickFirsts.add("**/libc++_shared.so")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

}

ksp {
    arg("room.generateKotlin", "true")
}

val verifySherpaTts = tasks.register("verifySherpaTts") {
    doLast {
        val aar = project.file("libs/sherpa-onnx-1.10.35.aar")
        if (!aar.isFile || aar.length() < 1_000_000L) {
            error(
                "Required Sherpa-ONNX 1.10.35 AAR is missing or invalid: ${aar.relativeTo(project.projectDir)}. " +
                    "Run scripts/setup_sherpa_tts.sh before building."
            )
        }
    }
}

val verifyWhisperConfiguration = tasks.register("verifyWhisperConfiguration") {
    doLast {
        val sourceDir = project.file("src/main/cpp/third_party/whisper.cpp")
        val nativeJni = project.file("src/main/cpp/WhisperJni.cpp")
        val modelStore = project.file("src/main/java/com/jarvis/ai/voice/WhisperModelStore.kt")
        require(nativeJni.isFile && modelStore.isFile) {
            "Whisper phase-1 native/JNI/model-store files are missing."
        }
        if (!sourceDir.resolve("CMakeLists.txt").isFile) {
            logger.warn("whisper.cpp source is not provisioned. Run scripts/setup_whisper.sh before a real STT build.")
        }
    }
}


val verifyRiveAsset = tasks.register("verifyRiveAsset") {
    doLast {
        val rive = project.file("src/main/res/raw/jarvis_orb.riv")
        if (!rive.isFile || rive.length() < 128L) {
            error(
                "Required local Rive asset is missing or invalid: ${rive.relativeTo(project.projectDir)}. " +
                    "Place the real jarvis_orb.riv in app/src/main/res/raw before building."
            )
        }
        logger.lifecycle("Rive asset verified: ${rive.relativeTo(project.projectDir)} (${rive.length()} bytes)")
    }
}

val verifyVoiceAssets = tasks.register("verifyVoiceAssets") {
    doLast {
        val assetsDir = project.file("src/main/assets")
        val requiredFiles = listOf(
            File(assetsDir, "melspectrogram.onnx"),
            File(assetsDir, "embedding_model.onnx"),
            File(assetsDir, "hey_jarvis_v0.1.onnx"),
            File(assetsDir, "jarvis/jarvis-high.onnx"),
            File(assetsDir, "jarvis/tokens.txt")
        )
        val missingFiles = requiredFiles.filterNot { it.isFile && it.length() > 0L }
        val espeakDir = File(assetsDir, "jarvis/espeak-ng-data")
        if (missingFiles.isNotEmpty() || !espeakDir.isDirectory) {
            val missing = missingFiles.joinToString(separator = "\n") { " - ${it.relativeTo(project.projectDir)}" }
            error(
                "Required offline voice assets are missing.\n" +
                    missing +
                    (if (!espeakDir.isDirectory) "\n - ${espeakDir.relativeTo(project.projectDir)}" else "") +
                    "\nRun scripts/setup_wakeword.sh and scripts/setup_jarvis_voice.sh before building."
            )
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifySherpaTts, verifyVoiceAssets, verifyWhisperConfiguration, verifyRiveAsset)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-process:2.11.0")
    implementation("androidx.core:core-ktx:1.19.0")

    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    // Offline Piper/VITS TTS engine. Sherpa-ONNX 1.10.35 uses
    // ONNX Runtime 1.18.0, matching OpenWakeWord 0.1.5.
    // This keeps one compatible ONNX Runtime version across both engines.
    implementation(files("libs/sherpa-onnx-1.10.35.aar"))
    // Real on-device wake-word detection via ONNX Runtime 1.18.0.
    implementation("xyz.rementia:openwakeword:0.1.5")

    // Rive Android runtime for the live JARVIS orb. Pin the patch version for reproducible builds.
    implementation("app.rive:rive-android:11.12.1")

    // JetBrains Koog: real agent orchestration and tool-calling on Android/JVM.
    // 1.1.1 is kept because it supports this project's minSdk 26 setup.
    implementation("ai.koog:koog-agents:1.1.1")

    // Google LiteRT-LM Kotlin API for on-device LLM inference.
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.16.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
