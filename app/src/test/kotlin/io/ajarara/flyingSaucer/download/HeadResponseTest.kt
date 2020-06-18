package io.ajarara.flyingSaucer.download

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.beInstanceOf
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*

internal class HeadResponseTest : StringSpec({

    "a non 200 should return Error.NotOk even if ETags are missing" {
        val headResponse = HeadResponse.of(201, mapOf())

        headResponse should beInstanceOf<HeadResponse.Error.NotOk>()
    }

    "multiple etags should be rejected since they're ambiguous" {
        val headResponse = HeadResponse.of(200, mapOf(
            "ETag" to listOf("First-ETag", "Second-ETag")
        ))

        headResponse should beInstanceOf<HeadResponse.Error.MultipleETags>()
    }

    "a single etag should be returned" {
        val headResponse = HeadResponse.of(200, mapOf(
            "SomeOtherHeader" to listOf("Arbitrary"),
            "ETag" to listOf("toBeReturned")
        ))

        headResponse as HeadResponse.ETag
        headResponse.etag shouldBe "toBeReturned"
    }
})