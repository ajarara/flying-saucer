package io.ajarara.flyingSaucer

import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCaseConfig
import io.kotest.matchers.shouldBe
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import retrofit2.Response
import java.net.HttpURLConnection

internal class ArchiveAPITest : StringSpec({

    "check HEADs against a known Accept-Range endpoint" {
        val response = ArchiveAPI.Impl.check(plan9)
            .blockingGet()

        response.code() shouldBe HttpURLConnection.HTTP_OK
        response.headers()["accept-ranges"] shouldBe "bytes"
    }

    "download returns only number of bytes we request" {
        val response = ArchiveAPI.Impl.uncheckedDownload(plan9, "bytes=0-1023")
            .blockingGet()

        response.code() shouldBe HttpURLConnection.HTTP_PARTIAL
        response.body()!!.size shouldBe 1024
    }

    "download returns the same etag as check does" {
        val (downloadResponse, headResponse) = ArchiveAPI.Impl.check(plan9)
            .flatMap { checkResponse ->
                ArchiveAPI.Impl.download(
                    plan9,
                    bytes = "bytes=256-1279",
                    etag = checkResponse.headers()["ETag"]!!
                ).map { downloadResponse -> checkResponse to downloadResponse }
            }
            .blockingGet()

        downloadResponse.headers()["ETag"] shouldBe headResponse.headers()["ETag"]
    }

    "a download request that has a range that exceeds content length returns a 416" {
        val unsatisfiableRequest = contentLength(plan9)
            .flatMap { contentLength ->
                ArchiveAPI.Impl.uncheckedDownload(
                    plan9,
                    "bytes=$contentLength-${contentLength+10}"
                )
            }
            .blockingGet()

        unsatisfiableRequest.code() shouldBe rangeUnsatisfiableCode
    }

    "a download request that spans across the content length returns a 206" {
        val satisfiedSpanningRequest = contentLength(plan9)
            .flatMap { contentLength ->
                ArchiveAPI.Impl.uncheckedDownload(
                    plan9,
                    "bytes=${contentLength-10}-${contentLength+10}"
                )
            }
            .blockingGet()


        satisfiedSpanningRequest.code() shouldBe HttpURLConnection.HTTP_PARTIAL
        satisfiedSpanningRequest.body()!!.size shouldBe 10
    }

    "a check request that has a range that exceeds content length returns a 416" {
        val unsatisfiableHead = contentLength(plan9)
            .flatMap { contentLength -> ArchiveAPI.Impl.check(plan9, "bytes=$contentLength-${contentLength+10}") }
            .blockingGet()

        unsatisfiableHead.code() shouldBe rangeUnsatisfiableCode
    }

    "a check request that spans across the content length returns a 206" {
        val satisfiedHead = contentLength(plan9)
            .flatMap { contentLength -> ArchiveAPI.Impl.check(plan9, "bytes=${contentLength-10}-${contentLength+10}")}
            .blockingGet()

        satisfiedHead.code() shouldBe HttpURLConnection.HTTP_PARTIAL
    }
}) {
    private companion object {
        const val plan9 = "Plan_9_from_Outer_Space_1959/Plan_9_from_Outer_Space_1959_512kb.mp4"

        // not available in the std lib
        const val rangeUnsatisfiableCode = 416

        fun contentLength(movie: String): Single<Long> = ArchiveAPI.Impl.check(movie)
            .map { response -> response.headers()["Content-Length"]!! }
            .map { contentLength -> contentLength.toLong() }

    }
}