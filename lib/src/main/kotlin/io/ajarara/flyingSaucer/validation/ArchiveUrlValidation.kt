package io.ajarara.flyingSaucer.validation

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.net.URI
import java.net.URISyntaxException

fun parseMovie(unsanitizedInput: String): Result<String, ArchiveUrlValidationError> {
    val uri = try {
        URI(unsanitizedInput)
    } catch (e: URISyntaxException) {
        return Err(ArchiveUrlValidationError.InvalidURLSyntax(unsanitizedInput))
    }

    if (uri.scheme != "https") {
        return Err(ArchiveUrlValidationError.IncorrectScheme(uri.scheme))
    }
    if (!uri.host.endsWith("archive.org")) {
        return Err(ArchiveUrlValidationError.UnknownHost(uri.host))
    }
    if (!uri.path.startsWith("/download/")) {
        return Err(ArchiveUrlValidationError.NotADownload(uri.path))
    }
    return Ok(uri.path.substringAfter("/download/"))
}

sealed class ArchiveUrlValidationError {
    class InvalidURLSyntax(val unsanitizedInput: String) : ArchiveUrlValidationError()
    class IncorrectScheme(val wrongScheme: String) : ArchiveUrlValidationError()
    class NotADownload(val path: String) : ArchiveUrlValidationError()
    class UnknownHost(val host: String) : ArchiveUrlValidationError()
}

