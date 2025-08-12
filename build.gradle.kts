plugins {
    kotlin("jvm") version "2.1.21"
    `maven-publish`
}

group = "org.key-project.devel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Do not deliver checkstyle as a dependency
    compileOnly("com.puppycrawl.tools:checkstyle:10.26.1")

    // but checkstyle is required for testing.
    testImplementation("com.puppycrawl.tools:checkstyle:10.26.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}