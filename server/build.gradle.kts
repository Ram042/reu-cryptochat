plugins {
    id("java")
    id("application")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    implementation(project(":lib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${rootProject.extra["jsonVersion"]}")
    implementation("org.bouncycastle:bcprov-jdk15on:${rootProject.extra["bouncycastleVersion"]}")
    implementation("org.jetbrains.xodus:xodus-openAPI:${rootProject.extra["xodusVersion"]}")
    implementation("org.jetbrains.xodus:xodus-environment:${rootProject.extra["xodusVersion"]}")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
//    implementation("io.javalin:javalin:${rootProject.extra["javalinVersion"]}")

    implementation("org.slf4j:slf4j-api:${rootProject.extra["slf4jVersion"]}")
    implementation("org.slf4j:slf4j-simple:${rootProject.extra["slf4jVersion"]}")

    testImplementation("org.testng:testng:${rootProject.extra["testngVersion"]}")
    testImplementation("org.assertj:assertj-core:${rootProject.extra["assertjVersion"]}")
    testImplementation(kotlin("test"))
}

//tasks.run<JavaExec> {
//    workingDir = project.layout.buildDirectory.asFile.get()
//    systemProperties["org.slf4j.simpleLogger.log.jetbrains.exodus.io.FileDataWriter"] = "off"
//}

tasks.test {
    useJUnitPlatform()
    workingDir = project.layout.buildDirectory.asFile.get()
    systemProperties["org.slf4j.simpleLogger.log.jetbrains.exodus.io.FileDataWriter"] = "off"
}
