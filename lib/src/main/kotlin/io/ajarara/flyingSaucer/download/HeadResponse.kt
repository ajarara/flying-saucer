package io.ajarara.flyingSaucer.download


sealed class HeadResponse {

    sealed class Error : HeadResponse() {
        class NotOk(val code: Int) : Error()

        object NoETag : Error()

        class MultipleETags(val etags: List<String>) : Error()
    }

    class ETag(val etag: String) : HeadResponse()

    companion object {

        fun of(code: Int, headers: Map<String, List<String>>): HeadResponse {
            val etags = headers["ETag"]
            return when {
                code != 200 -> Error.NotOk(code)
                etags.isNullOrEmpty() -> Error.NoETag
                etags.size > 1 -> Error.MultipleETags(etags)
                else -> ETag(etags.single())
            }
        }
    }
}