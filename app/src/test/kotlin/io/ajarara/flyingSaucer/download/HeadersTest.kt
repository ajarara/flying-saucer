package io.ajarara.flyingSaucer.download

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class HeadersTest : StringSpec({
    "bytesOf should respect be right inclusive" {
        val byteHeaders = Headers.bytesOf(chunkNo = 1, chunkSize = 15)

        byteHeaders shouldBe "bytes=15-29"
    }
})