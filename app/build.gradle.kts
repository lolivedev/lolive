import com.android.build.OutputFile
import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Copy

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

val nativeAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
val cmakeFile = file("src/main/cpp/CMakeLists.txt")
val hasNativeSource = cmakeFile.exists()
val usePrebuiltNative = providers.gradleProperty("usePrebuiltNative").orNull == "true"
val enableCmakeNative = hasNativeSource && !usePrebuiltNative

android {
    namespace = "com.ho.lolive"
    compileSdk = 34
    ndkVersion = "29.0.14206865"

    val signingStoreFile = System.getenv("SIGNING_STORE_FILE")
    val signingStorePassword = System.getenv("SIGNING_STORE_PASSWORD")
    val signingKeyAlias = System.getenv("SIGNING_KEY_ALIAS")
    val signingKeyPassword = System.getenv("SIGNING_KEY_PASSWORD")
    val hasReleaseSigning = !signingStoreFile.isNullOrBlank() &&
        !signingStorePassword.isNullOrBlank() &&
        !signingKeyAlias.isNullOrBlank() &&
        !signingKeyPassword.isNullOrBlank()

    defaultConfig {
        applicationId = "com.ho.lolive"
        minSdk = 24
        targetSdk = 34
        versionCode = 23
        versionName = "1.21"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "BASE_URL", "\"https://localhost/\"")
        buildConfigField("boolean", "ENABLE_HTTP_LOG", "true")

        if (enableCmakeNative) {
            externalNativeBuild {
                cmake {
                    cppFlags += "-std=c++17"
                }
            }
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(signingStoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("boolean", "ENABLE_HTTP_LOG", "true")
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("boolean", "ENABLE_HTTP_LOG", "false")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            if (enableCmakeNative) {
                // Avoid duplicate .so merge when both CMake output and jniLibs exist.
                jniLibs.setSrcDirs(emptyList<String>())
            } else {
                jniLibs.srcDirs("src/main/jniLibs")
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include(*nativeAbis.toTypedArray())
            isUniversalApk = true
        }
    }

    if (enableCmakeNative) {
        externalNativeBuild {
            cmake {
                path = cmakeFile
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    applicationVariants.all {
        outputs.all {
            val output = this as BaseVariantOutputImpl
            val abi = output.getFilter(OutputFile.ABI) ?: "universal"
            val safeVersionName = (versionName ?: "0.0.0").replace(Regex("[^0-9A-Za-z._-]"), "_")
            output.outputFileName = "lolive-v${safeVersionName}-${versionCode}-${buildType.name}-$abi.apk"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    implementation("androidx.paging:paging-runtime-ktx:3.2.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-paging:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("io.coil-kt:coil:2.6.0")

    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("androidx.media3:media3-datasource-rtmp:1.3.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("com.google.truth:truth:1.4.2")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/detekt.yml"))
}

tasks.register<Copy>("exportNativeSo") {
    group = "build"
    description = "Build release native library and copy liblolive_native.so into src/main/jniLibs for all ABIs."
    onlyIf { hasNativeSource }
    dependsOn("externalNativeBuildRelease")
    val abis = nativeAbis.toSet()
    from(layout.buildDirectory.dir("intermediates/cxx/RelWithDebInfo")) {
        include("**/obj/**/liblolive_native.so")
        includeEmptyDirs = false
        eachFile {
            val abi = relativePath.segments.firstOrNull { it in abis }
            if (abi == null) {
                exclude()
            } else {
                relativePath = RelativePath(true, abi, "liblolive_native.so")
            }
        }
    }
    into(layout.projectDirectory.dir("src/main/jniLibs"))
}

