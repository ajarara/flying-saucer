package io.ajarara.flyingSaucer

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.haveSize
import io.kotest.matchers.maps.haveSize
import io.kotest.matchers.sequences.haveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import okhttp3.ResponseBody
import retrofit2.Response

internal class ArchiveAPITest : StringSpec({

    val plan9 = "Plan_9_from_Outer_Space_1959/Plan_9_from_Outer_Space_1959_512kb.mp4"

    "check HEADs against a known Accept-Range endpoint" {
        val headers = ArchiveAPI.Impl.check(plan9)
            .blockingGet()
            .headers()

        headers["accept-ranges"] shouldBe "bytes"
    }

    "download returns only number of bytes we request" {
        val response = ArchiveAPI.Impl.download(plan9, "bytes=0-1023")
            .blockingGet()

        response.code() shouldBe 206
        response.body()!!.size shouldBe 1024
    }

    "download returns the same etag as check does" {
        val (downloadResponse, headResponse) =
            Single.zip(
                ArchiveAPI.Impl.download(plan9, "bytes=0-1023"),
                ArchiveAPI.Impl.check(plan9),
                BiFunction { downloadResponse: Response<ByteArray>, headResponse: Response<Void> ->
                    downloadResponse to headResponse
                }
            ).blockingGet()

        downloadResponse.headers()["ETag"] shouldBe headResponse.headers()["ETag"]
    }

    "a download request that has a range that exceeds content length returns a 416" {
        val unsatisfiableRequest = ArchiveAPI.Impl.check(plan9)
            .map { response -> response.headers()["Content-Length"] }
            .flatMap { contentLength -> ArchiveAPI.Impl.download(plan9, "bytes=$contentLength-${contentLength+10}")}
            .blockingGet()

        unsatisfiableRequest.code() shouldBe 416
    }
})