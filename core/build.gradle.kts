plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":map-script"))

    implementation("org.ow2.asm:asm-tree:9.8")
    compileOnly("org.vineflower:vineflower:1.11.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}