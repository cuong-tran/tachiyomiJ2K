object Versions {
    const val NUCLEUS = "3.0.0"
    const val OSS_LICENSE = "17.0.0"
    const val ROBO_ELECTRIC = "3.1.4"
    const val RX_BINDING = "1.0.1"
    const val androidAppCompat = "1.1.0"
    const val androidWorkManager = "2.4.0"
    const val changelog = "2.1.0"
    const val coil = "1.1.1"
    const val conductor = "2.1.5"
    const val directionalViewPager = "a844dbca0a"
    const val diskLruCache = "2.0.2"
    const val fastAdapter = "5.4.1"
    const val filePicker = "2.5.2"
    const val firebase = "17.5.0"
    const val firebaseCrashlytics = "17.2.1"
    const val googleServices = "4.3.3"
    const val gradleVersions = "0.29.0"
    const val injekt = "65b0440"
    const val junit = "4.13"
    const val kotlin = "1.6.20"
    const val kotson = "2.5.0"
    const val mockito = "1.10.19"
    const val moshi = "1.9.3"
    const val nucleus = "3.0.0"
    const val okhttp = "4.9.1"
    const val okio = "2.10.0"
    const val photoView = "2.3.0"
    const val reactiveNetwork = "0.13.0"
    const val rxAndroid = "1.2.1"
    const val rxBinding = "1.0.1"
    const val rxJava = "1.3.8"
    const val rxRelay = "1.2.0"
    const val subsamplingImageScale = "93d74f0"
    const val systemUiHelper = "1.0.0"
    const val tapTargetView = "1.13.0"
    const val unifile = "e9ee588"
    const val viewStatePagerAdapter = "1.1.0"
    const val viewToolTip = "1.2.2"
    const val kotlinter = "3.4.4"
}

object LegacyPluginClassPath {
    const val googleServices = "com.google.gms:google-services:${Versions.googleServices}"
    const val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlinSerializations = "org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}"
    const val fireBaseCrashlytics = "com.google.firebase:firebase-crashlytics-gradle:2.3.0"
}

object AndroidVersions {
    const val compileSdk = 31
    const val minSdk = 23
    const val targetSdk = 30
    const val versionCode = 88
    const val versionName = "1.5.0"
    const val ndk = "23.1.7779620"
}

object Plugins {
    const val androidApplication = "com.android.application"
    const val firebaseCrashlytics = "com.google.firebase.crashlytics"
    const val googleServices = "com.google.gms.google-services"
    const val kapt = "kapt"
    const val kotlinParcelize = "kotlin-parcelize"
    const val kotlinAndroid = "android"
    const val jetbrainsKotlin = "org.jetbrains.kotlin.android"
    const val kotlinSerialization = "org.jetbrains.kotlin.plugin.serialization"
    val gradleVersions = PluginClass("com.github.ben-manes.versions", Versions.gradleVersions)
    val kotlinter = PluginClass("org.jmailen.kotlinter", Versions.kotlinter)
}

data class PluginClass(val name: String, val version: String)

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
