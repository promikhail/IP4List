plugins {
    alias(projLibs.plugins.kotlin.jvm)
    application
}

group = "com.promikhail.research.ip4list.analyzer"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(projLibs.versions.java.version.get().toInt())
}

dependencies {
    implementation(project(":tools"))

    implementation(projLibs.kotlinx.coroutines.core.jvm)
}

application {
    mainClass = "com.promikhail.research.ip4list.analyzer.MainKt"
}