plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.sf.jopt-simple:jopt-simple:6.0-alpha-3")
    implementation("org.ow2.asm:asm-tree:9.8")
    implementation("org.vineflower:vineflower:1.11.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}