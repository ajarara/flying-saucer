package io.ajarara.flyingSaucer.download

object Headers {
    fun bytesOf(chunkNo: Int, chunkSize: Int): String {
        val start = chunkNo * chunkSize
        return "bytes=${start}-${start + chunkSize - 1}"
    }
}