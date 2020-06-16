package io.ajarara.flyingSaucer.response

import java.net.HttpURLConnection

sealed class DownloadResult {

    object Empty : DownloadResult()

    class InvalidETag(val number: Int) : DownloadResult()

    class Chunk(val number: Int, val data: ByteArray) : DownloadResult()

    object Unknown : DownloadResult()

    companion object {
        fun from(code: Int, chunkNo: Int, body: ByteArray?): DownloadResult {
            return when (code) {
                416 -> Empty
                HttpURLConnection.HTTP_PARTIAL -> Chunk(chunkNo, body!!)
                HttpURLConnection.HTTP_PRECON_FAILED -> InvalidETag(chunkNo)
                else -> Unknown
            }
        }
    }
}