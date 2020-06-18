package io.ajarara.flyingSaucer.download

import java.net.HttpURLConnection

sealed class ChunkResult {

    object Empty : ChunkResult()

    class InvalidETag(val chunkNo: Int) : ChunkResult()

    class Chunk(val number: Int, val data: ByteArray) : ChunkResult()

    class UnknownCode(val code: Int) : ChunkResult()

    companion object {
        fun from(code: Int, chunkNo: Int, body: ByteArray?): ChunkResult {
            return when (code) {
                416 -> Empty
                HttpURLConnection.HTTP_PARTIAL -> Chunk(chunkNo, body!!)
                HttpURLConnection.HTTP_PRECON_FAILED -> InvalidETag(chunkNo)
                else -> UnknownCode(code)
            }
        }
    }
}