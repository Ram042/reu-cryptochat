plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    api("org.bouncycastle:bcprov-debug-jdk18on:${rootProject.extra["bouncycastleVersion"]}")
    implementation("org.jetbrains:annotations:${rootProject.extra["annotationsVersion"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${rootProject.extra["jsonVersion"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("com.google.guava:guava:${rootProject.extra["guavaVersion"]}")

    testImplementation("org.slf4j:slf4j-simple:2.0.12")
    testImplementation("org.slf4j:slf4j-simple:${rootProject.extra["slf4jVersion"]}")
    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:${rootProject.extra["assertjVersion"]}")
}

tasks.test {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
}

kotlin {
    jvmToolchain(21)
}