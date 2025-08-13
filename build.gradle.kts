plugins {
    kotlin("jvm") version "2.2.0"
    `maven-publish`
    signing
}

group = "org.key-project.devel"
version = "1.17-SNAPSHOT"
description = "A concise description of my library"


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

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "checkstyle-key-extensions"
            from(components["java"])

            pom {
                url = "http://github.com/wadoon/checkstyle-key-extensions"
                properties = mapOf(
                    "myProp" to "value",
                    "prop.with.dots" to "anotherValue"
                )
                licenses {
                    license {
                        name = "GPLv2-or-later"
                        url = "https://www.gnu.org/licenses/gpl-2.0.html"
                    }
                }
                /*developers {
                    developer {
                        id = ""
                        name = "John Doe"
                        email = "john.doe@example.com"
                    }
                }*/
                scm {
                    connection = "scm:git:git://github.com/wadoon/checkstyle-key-extensions.git"
                    developerConnection = "scm:git:git://github.com/wadoon/checkstyle-key-extensions.git"
                    url = "http://github.com/wadoon/checkstyle-key-extensions.git"
                }
            }
        }
    }
    repositories {
        maven {
            credentials(PasswordCredentials::class) {
                username = project.properties["keylab.username"].toString()
                password = project.properties["keylab.token"].toString()
            }

            val releasesRepoUrl = uri("https://git.key-project.org/api/v4/projects/35/packages/maven")
            val snapshotsRepoUrl = uri("https://git.key-project.org/api/v4/projects/35/packages/maven")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}