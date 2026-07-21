plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.quizedguy.genghealth"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.quizedguy.genghealth"
        minSdk = 24
        targetSdk = 36
        versionCode = 27
        versionName = "1.25"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("genghealth-release.jks")
            storePassword = "genghealth123"
            keyAlias = "genghealth-alias"
            keyPassword = "genghealth123"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            isCrunchPngs = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    constraints {
        implementation(libs.androidx.fragment) {
            because("Transitive dependency fragment:1.1.0 is flagged as outdated by Google Play Console")
        }
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    

    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.ads)
    implementation(libs.play.services.base)
    implementation(libs.play.age.signals)
    
    // AdMob Mediation Adapters and Support Libraries
    implementation(libs.play.services.ads.mediation.inmobi)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.play.services.ads.mediation.meta)
    implementation(libs.unity.ads)
    implementation(libs.play.services.ads.mediation.unity)
    implementation(libs.play.services.ads.mediation.ironsource)
    implementation(libs.play.services.ads.mediation.vungle)
    implementation(libs.user.messaging.platform)
    implementation(libs.play.services.ads.mediation.chartboost)

    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}