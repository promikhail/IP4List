plugins {
    alias(projLibs.plugins.kotlin.jvm)
}

group = "com.promikhail.research.ip4list.tools"
version = "1.0.0"

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(projLibs.versions.java.version.get().toInt())
}

dependencies {
    implementation(projLibs.kotlinx.coroutines.core.jvm)

    testImplementation(projLibs.kotlin.test)
    testImplementation(projLibs.kotlinx.coroutines.test)
}