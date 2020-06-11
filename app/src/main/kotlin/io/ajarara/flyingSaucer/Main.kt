package io.ajarara.flyingSaucer

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import java.net.URI

object Main : CliktCommand() {

    val archiveUrl by option(help="Mp4 to download from archive.org")
        .default("https://archive.org/download/Plan_9_from_Outer_Space_1959/Plan_9_from_Outer_Space_1959_512kb.mp4")

    override fun run() {
    }
}

fun main() = Main.run()


