plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":map-script"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.ow2.asm:asm-tree:9.8")
    implementation("org.ow2.asm:asm-commons:9.8")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.26.4")
    implementation("org.vineflower:vineflower:1.11.1")
    testImplementation("io.mockk:mockk:1.14.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}