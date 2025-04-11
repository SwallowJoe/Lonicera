plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    // alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(libs.coroutines.core)
    api(libs.ktor.client.core)
    api(libs.ktor.client.auth)
    implementation(libs.ktor.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.serialization.json)
    implementation(libs.ktor.client.logging)
}