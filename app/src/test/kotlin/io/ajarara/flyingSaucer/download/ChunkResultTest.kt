package io.ajarara.flyingSaucer.download

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.beInstanceOf
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import java.net.HttpURLConnection

internal class ChunkResultTest : StringSpec({

    "range unsatisfiable should be empty" {
        val result = ChunkResult.from(416, 30, null)

        result shouldBe ChunkResult.Empty
    }

    "a http partial should require a body" {
        shouldThrowAny {
            ChunkResult.from(HttpURLConnection.HTTP_PARTIAL, 1, null)
        }
    }

    "a http partial should return the body ByteArray" {
        val result = ChunkResult.from(HttpURLConnection.HTTP_PARTIAL, 1, byteArrayOf(1, 2))

        result as ChunkResult.Chunk
        result.data shouldBe byteArrayOf(1, 2)
    }

    "a precondition failure should return an invalid etag" {
        val result = ChunkResult.from(HttpURLConnection.HTTP_PRECON_FAILED, 10, null)

        result should beInstanceOf<ChunkResult.InvalidETag>()
    }

})