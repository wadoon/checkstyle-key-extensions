plugins {
    kotlin("jvm") version "2.1.21"
}

group = "org.key-project.devel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api("com.puppycrawl.tools:checkstyle:10.26.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}