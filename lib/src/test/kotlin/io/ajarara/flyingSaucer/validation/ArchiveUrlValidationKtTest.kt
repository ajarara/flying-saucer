package io.ajarara.flyingSaucer.validation

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.expect
import com.github.michaelbull.result.expectError
import io.kotest.core.spec.style.StringSpec

import io.kotest.matchers.shouldBe

internal class ArchiveUrlValidationKtTest: StringSpec({

    "an invalid URI returns an invalid uri syntax err" {
        val input = "here's some command line prose!"
        val parsed = parseMovie(input)

        parsed.expectError { "Prose is not a valid archive.org movie link" }
        val invalidSyntax = parsed.error as ArchiveUrlValidationError.InvalidURLSyntax
        invalidSyntax.inputString shouldBe input
    }

    "a valid URI that does not point to archive.org is unknown" {
        val input = "https://www.youtube.com/watch?v=RYsTlfhDSDY"
        val parsed = parseMovie(input)

        parsed.expectError { "Only archive.org links should be allowed" }
        val unknownHost = parsed.error as ArchiveUrlValidationError.UnknownHost
        unknownHost.host shouldBe "www.youtube.com"
    }

    "a valid archive.org URI that does not use https is rejected" {
        val input = "http://archive.org/download/happy_city/happy_city.m4v"
        val parsed = parseMovie(input)

        parsed.expectError { "Http should not be an allowed scheme" }
        val incorrectScheme = parsed.error as ArchiveUrlValidationError.IncorrectScheme
        incorrectScheme.wrongScheme shouldBe "http"
    }

    "a valid archive.org URI returns the path without the leading /download" {
        val input = "https://archive.org/download/happy_city/happy_city.m4v"
        val parsed = parseMovie(input)

        parsed as Ok
        parsed.expect { "Archive.org download links should be valid to parse" }
        parsed.value shouldBe "happy_city/happy_city.m4v"
    }
})