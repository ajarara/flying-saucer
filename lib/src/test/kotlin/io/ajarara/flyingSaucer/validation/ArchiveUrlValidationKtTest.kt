package io.ajarara.flyingSaucer.validation

import com.github.michaelbull.result.Err
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

internal class ArchiveUrlValidationKtTest: StringSpec({

    "an invalid URI returns an invalid uri syntax err" {
        val input = "here's some command line prose!"
        val parsed = parseMovie(input)

        parsed as Err
        val invalidSyntax = parsed.error as ArchiveUrlValidationError.InvalidURLSyntax
        invalidSyntax.unsanitizedInput shouldBe input
    }
})