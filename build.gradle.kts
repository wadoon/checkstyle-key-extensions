plugins {
    kotlin("jvm") version "2.2.0"
    `maven-publish`
}

group = "org.key-project.devel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Do not deliver checkstyle as a dependency
    compileOnly("com.puppycrawl.tools:checkstyle:11.0.0")

    // but checkstyle is required for testing.
    testImplementation("com.puppycrawl.tools:checkstyle:11.0.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}