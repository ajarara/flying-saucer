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
    implementation(Data.result)
    implementation(project(":lib"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.ajalt:clikt:2.7.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.7.1")
    implementation("com.squareup.retrofit2:converter-scalars:2.7.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.10")

    testImplementation(Testing.jupiter)
    testImplementation(Testing.KoTest.runner)
    testImplementation(Testing.KoTest.assertions)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    withType(Test::class.java) {
        useJUnitPlatform()
    }
}