
object Testing {
    val jupiter = "org.junit.jupiter:junit-jupiter:5.6.0"

    object KoTest {
        private val version = "4.0.6"
        val runner = "io.kotest:kotest-runner-junit5-jvm:$version"
        val assertions = "io.kotest:kotest-assertions-core-jvm:$version"
    }
}
