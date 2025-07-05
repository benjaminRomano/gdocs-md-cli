plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.bromano"
version = "1.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("com.google.api-client:google-api-client:2.8.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
    implementation("com.google.apis:google-api-services-docs:v1-rev20250325-2.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks {
    shadowJar {
        manifest {
            attributes("Main-Class" to "com.bromano.CliKt")
        }
        archiveBaseName.set("gdocs-md")
        archiveVersion.set("")
        archiveClassifier.set("")
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
