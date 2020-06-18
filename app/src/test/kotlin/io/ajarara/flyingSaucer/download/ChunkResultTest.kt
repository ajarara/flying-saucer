package io.ajarara.flyingSaucer.download

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.beInstanceOf
import io.kotest.matchers.sequences.containAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.containInOrder
import okhttp3.HttpUrl
import java.net.HttpURLConnection

internal class DownloadResultTest : StringSpec({

    "range unsatisfiable should be empty" {
        val result = DownloadResult.from(416, 30, null)

        result shouldBe DownloadResult.Empty
    }

    "a http partial should require a body" {
        shouldThrowAny {
            DownloadResult.from(HttpURLConnection.HTTP_PARTIAL, 1, null)
        }
    }

    "a http partial should return the body ByteArray" {
        val result = DownloadResult.from(HttpURLConnection.HTTP_PARTIAL, 1, byteArrayOf(1, 2))

        result as DownloadResult.Chunk
        result.data shouldBe byteArrayOf(1, 2)
    }

    "a precondition failure should return an invalid etag" {
        val result = DownloadResult.from(HttpURLConnection.HTTP_PRECON_FAILED, 10, null)

        result should beInstanceOf<DownloadResult.InvalidETag>()
    }

})