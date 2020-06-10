package io.ajarara.flyingSaucer

import com.github.ajalt.clikt.core.CliktCommand

object Main : CliktCommand() {
    override fun run() {
        println("Hello!")
    }
}

fun main() = Main.run()


