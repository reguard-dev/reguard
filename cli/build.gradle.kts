plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version "9.0.0-beta15"
}

repositories {
    mavenCentral()
}

dependencies {
    shadow(project(":core"))

    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("com.github.ajalt.clikt:clikt-markdown:5.0.3")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("fatjar")
}

kotlin {
    jvmToolchain(21)
}