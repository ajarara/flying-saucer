plugins {
    kotlin("jvm")
    id("application")
}

repositories {
    mavenCentral()
}

application {
    mainClassName = "io.ajarara.flyingSaucer.MainKt"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.ajalt:clikt:2.7.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.7.1")
    implementation("com.squareup.retrofit2:converter-scalars:2.7.1")
    // implementation("com.squareup.retrofit2:converter-jackson:2.7.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.10")

    testImplementation(Testing.jupiter)
    testImplementation(Testing.KoTest.runner)
    testImplementation(Testing.KoTest.assertions)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    withType(Test::class.java) {
        useJUnitPlatform()
    }
}