package io.ajarara.flyingSaucer.download

import java.net.HttpURLConnection

sealed class ChunkResult {

    object Empty : ChunkResult()

    object InvalidETag : ChunkResult()

    class Chunk(val number: Int, val data: ByteArray) : ChunkResult()

    object Unknown : ChunkResult()

    companion object {
        fun from(code: Int, chunkNo: Int, body: ByteArray?): ChunkResult {
            return when (code) {
                416 -> Empty
                HttpURLConnection.HTTP_PARTIAL -> Chunk(chunkNo, body!!)
                HttpURLConnection.HTTP_PRECON_FAILED -> InvalidETag
                else -> Unknown
            }
        }
    }
}