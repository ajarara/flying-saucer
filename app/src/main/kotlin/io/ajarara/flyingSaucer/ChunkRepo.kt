package io.ajarara.flyingSaucer

import io.ajarara.flyingSaucer.download.HeadResponse
import java.io.File
import java.util.concurrent.ConcurrentHashMap

interface ChunkRepo {
    fun get(chunkNo: Int): ByteArray?
    fun set(chunkNo: Int, chunk: ByteArray)
    fun chunks(): Sequence<ByteArray>

    fun firstGap(): Int {
        var chunkNo = 0
        while (get(chunkNo) != null) {
            chunkNo++
        }
        return chunkNo
    }
}

class InMemoryChunkRepo : ChunkRepo {
    private val chunkMap: MutableMap<Int, ByteArray> = ConcurrentHashMap()

    override fun get(chunkNo: Int): ByteArray? = chunkMap[chunkNo]

    override fun set(chunkNo: Int, chunk: ByteArray) {
        check(chunkNo !in chunkMap) {
            "Overwriting chunk $chunkNo in memory! This is an error!"
        }
        chunkMap[chunkNo] = chunk
    }

    override fun chunks(): Sequence<ByteArray> {
        val snapshot = chunkMap.toMap()
        return sequence {
            for (i in 0 until snapshot.size) {
                val chunk = requireNotNull(snapshot[i]) {
                    "Missing chunk $i of ${snapshot.size}"
                }

                yield(chunk)
            }
        }
    }
}

class DiskBackedChunkRepo(
    topLevelTmpDir: File,
    private val name: String,
    private val etag: HeadResponse.ETag
) : ChunkRepo {
    private val root: File = File(topLevelTmpDir.path, name).apply { mkdir() }

    override fun get(chunkNo: Int): ByteArray? = chunkFile(chunkNo).run {
        if (exists()) {
            readBytes()
        } else {
            null
        }
    }

    override fun set(chunkNo: Int, chunk: ByteArray) =
        chunkFile(chunkNo).run {
            val freshFile = createNewFile()
            check(freshFile) {
                "Overwriting chunk $chunkNo in ${this@DiskBackedChunkRepo.name}, this is an error!"
            }
            writeBytes(chunk)
        }

    override fun chunks(): Sequence<ByteArray> {
        val lastChunk = root.listFiles()!!
            .map { it.extension.toInt() }
            .max()
            ?: return emptySequence()

        return sequence {
            for (i in 0..lastChunk) {
                val chunk = requireNotNull(get(i)) {
                    "Missing chunk $i of $lastChunk on disk! Chunks can be found in ${root.path}"
                }
                yield(chunk)
            }
        }
    }

    private fun chunkFile(chunkNo: Int) = File(root.path, "${etag.etag}.$chunkNo")
}