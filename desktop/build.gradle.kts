plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(project(":lib"))
    implementation("org.jetbrains:annotations:${rootProject.extra["annotationsVersion"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${rootProject.extra["jsonVersion"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    runtimeOnly("androidx.annotation:annotation:1.8.0")

    implementation("org.slf4j:slf4j-simple:${rootProject.extra["slf4jVersion"]}")
    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:${rootProject.extra["assertjVersion"]}")

    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")

    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.uiTooling)
    implementation(compose.ui)
    implementation(compose.components.resources)
    implementation(compose.compiler.auto)
    implementation(compose.desktop.currentOs)
    implementation(compose.runtimeSaveable)
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
        mainClass = "desktop.AppKt"
        nativeDistributions {
            includeAllModules = true
            buildTypes.release {
                proguard {
                    this.isEnabled = false
                }
            }
        }
    }
}
