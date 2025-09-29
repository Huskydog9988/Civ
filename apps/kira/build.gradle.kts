plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "net.civmc.kira"
version = "2.1.1"
description = "Kira"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.json)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.jda)
    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.rabbitmq.client)
    implementation(libs.jsoup)
    implementation(libs.java.webSocket)
    implementation(libs.commons.collections4)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hoplite.core)
    implementation(libs.hoplite.yaml)

    implementation(libs.koin.core)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit5)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("net.civmc.kira.KiraKt")
}
