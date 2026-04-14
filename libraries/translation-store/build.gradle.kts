plugins {
    id("java")
}

group = "net.civmc.translation"
version = "1.0.0"

dependencies {
    implementation(libs.bundles.adventure)
    implementation(libs.gson)
    implementation(libs.slf4j.api)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21) // or your version
    }
}
