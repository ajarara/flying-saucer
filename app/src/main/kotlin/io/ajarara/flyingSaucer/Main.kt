package io.ajarara.flyingSaucer

import com.github.ajalt.clikt.core.CliktCommand

object Main : CliktCommand() {

    override fun run() {
        // https://archive.org/download/Plan_9_from_Outer_Space_1959/Plan_9_from_Outer_Space_1959_512kb.mp4
        println("Hello!")
    }
}

fun main() = Main.run()


