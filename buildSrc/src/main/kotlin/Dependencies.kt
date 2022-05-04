object Versions {
    const val NUCLEUS = "3.0.0"
    const val OSS_LICENSE = "17.0.0"
    const val ROBO_ELECTRIC = "3.1.4"
    const val RX_BINDING = "1.0.1"
    const val kotlin = "1.6.20"
}

object LegacyPluginClassPath {
    const val googleServices = "com.google.gms:google-services:4.3.10"
    const val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val kotlinSerializations = "org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}"
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
    const val googleServices = "com.google.gms.google-services"
    const val kapt = "kapt"
    const val kotlinParcelize = "kotlin-parcelize"
    const val kotlinAndroid = "android"
    const val jetbrainsKotlin = "org.jetbrains.kotlin.android"
    const val kotlinSerialization = "org.jetbrains.kotlin.plugin.serialization"
    val gradleVersions = PluginClass("com.github.ben-manes.versions", "0.42.0")
    val kotlinter = PluginClass("org.jmailen.kotlinter", "3.10.0")
}

data class PluginClass(val name: String, val version: String)

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
