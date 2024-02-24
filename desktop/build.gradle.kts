import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(project(":lib"))
    implementation("org.bouncycastle:bcprov-jdk15on:${rootProject.extra["bouncycastleVersion"]}")
    implementation("com.google.guava:guava:${rootProject.extra["guavaVersion"]}")
    implementation("io.seruco.encoding:base62:${rootProject.extra["base62Version"]}")
    implementation("org.jetbrains:annotations:${rootProject.extra["annotationsVersion"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${rootProject.extra["jsonVersion"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    implementation("org.jetbrains.exposed:exposed-core:${rootProject.extra["exposedVersion"]}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${rootProject.extra["exposedVersion"]}")
    implementation("org.jetbrains.exposed:exposed-dao:${rootProject.extra["exposedVersion"]}")
    implementation("org.xerial:sqlite-jdbc:${rootProject.extra["sqliteJdbcVersion"]}")

    testImplementation("org.slf4j:slf4j-simple:${rootProject.extra["slf4jVersion"]}")
    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:${rootProject.extra["assertjVersion"]}")

    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.uiTooling)
    implementation(compose.ui)
    @OptIn(ExperimentalComposeLibrary::class)
    implementation(compose.components.resources)
    implementation(compose.desktop.currentOs)
}

tasks.sorted().forEach {
    println("$it")
}

tasks.test {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
}
kotlin {
    jvmToolchain(21)
}


compose.desktop {
    application {
        mainClass = "MainKt"
    }
}
