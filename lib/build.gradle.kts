plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(Data.result)

    testImplementation(Testing.KoTest.runner)
    testImplementation(Testing.KoTest.assertions)
    testImplementation(Testing.jupiter)
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