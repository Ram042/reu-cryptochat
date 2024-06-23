plugins {
    id("java")
    id("application")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.ben-manes.versions") version "0.51.0"
    id("io.ktor.plugin") version "2.3.12"
    id("idea")
}

dependencies {
    implementation(project(":lib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${rootProject.extra["jsonVersion"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.assertj:assertj-core:${rootProject.extra["assertjVersion"]}")
    testImplementation(kotlin("test"))

    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    testImplementation("io.ktor:ktor-client-content-negotiation:2.1.1")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    implementation("org.slf4j:slf4j-simple:${rootProject.extra["slf4jVersion"]}")
}

application {
    mainClass = "server.ServerKt"
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.run<JavaExec> {
    workingDir = project.layout.buildDirectory.asFile.get()
}

tasks.test {
    useJUnitPlatform()
    workingDir = project.layout.buildDirectory.asFile.get()
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
