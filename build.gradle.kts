plugins {
    id("com.github.ben-manes.versions") version "0.51.0"
    id("org.jetbrains.kotlin.jvm") version "1.9.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21" apply false
    id("org.jetbrains.compose") version "1.5.12" apply false
    id("org.springframework.boot") version "3.2.1" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    id("idea")
}

ext["bouncycastleVersion"] = "1.70"
ext["guavaVersion"] = "33.0.0-jre"
ext["slf4jVersion"] = "1.7.32"
ext["urlbuilderVersion"] = "2.0.9"
ext["base62Version"] = "0.1.3"
ext["javalinVersion"] = "4.5.0"
ext["xodusVersion"] = "2.0.1"
ext["annotationsVersion"] = "24.1.0"
ext["protoobjVersion"] = "2.2"
ext["jsonVersion"] = "1.6.3"
ext["nitriteVersion"] = "3.4.4"
ext["jacksonVersion"] = "2.13.2"
ext["picocliVersion"] = "4.6.3"
ext["mockitoVersion"] = "4.5.1"
ext["testngVersion"] = "7.5"
ext["assertjVersion"] = "3.25.3"
ext["exposedVersion"] = "0.47.0"
ext["sqliteJdbcVersion"] = "3.45.1.0"

subprojects {
    repositories {
        mavenCentral()
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
