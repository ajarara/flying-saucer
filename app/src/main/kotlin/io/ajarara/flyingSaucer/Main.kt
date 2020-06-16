package io.ajarara.flyingSaucer

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.michaelbull.result.*
import io.ajarara.flyingSaucer.response.DownloadResult
import io.ajarara.flyingSaucer.validation.ArchiveUrlValidationError
import io.ajarara.flyingSaucer.validation.parseMovie
import io.reactivex.Maybe
import io.reactivex.Observable
import retrofit2.Response
import java.io.File
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

object Main : CliktCommand() {

    private val tempDir = File(System.getProperty("java.io.tmpdir"))

    private val archiveUrl: String by option(help = "Mp4 to download from archive.org")
        .default("https://archive.org/download/Plan_9_from_Outer_Space_1959/Plan_9_from_Outer_Space_1959_512kb.mp4")

    private val concurrentRequestMax: Int by option(help = "Number of requests to run simultaneously")
        .int()
        .default(32)

    private val noCache: Boolean by option(help = "Do not store partially downloaded files on disk")
        .flag("--no-cache")
        .validate { shouldCache ->
            if (shouldCache) {
                require(tempDir.exists()) {
                    "This platform does not have a temporary directory, and cannot cache chunks."
                }
                require(tempDir.isDirectory) {
                    "This platform's temp directory is not a directory, and cannot cache chunks."
                }
                require(tempDir.canWrite()) {
                    "This platform's temp directory is not writeable, and cannot cache chunks."
                }
            }
        }

    private const val chunkSize = 16384

    override fun run() {
        parseMovie(archiveUrl)
            .mapError {
                when (it) {
                    is ArchiveUrlValidationError.InvalidURLSyntax ->
                        println("Could not convert ${it.inputString}: ${it.reason}")
                    is ArchiveUrlValidationError.MissingScheme ->
                        println("Could not extract scheme from archiveUrl. Is this a web URL?")
                    is ArchiveUrlValidationError.IncorrectScheme ->
                        println("Only https is supported. Identified scheme: ${it.wrongScheme}")
                    is ArchiveUrlValidationError.NotADownload ->
                        println("Archive.org URL not a download link: ${it.path}")
                    is ArchiveUrlValidationError.UnknownHost ->
                        println("Only downloads from Archive.org are supported")
                }
                exitProcess(-1)
            }
            .map { download(it) }
    }

    private fun download(movie: String) {
        print("Checking if the download '$movie' exists: ")
        val headResponse = ArchiveAPI.Impl.check(movie)
            .blockingGet()
        if (headResponse.code() != 200) {
            println("It does not. Exiting.")
            exitProcess(-1)
        }

        val etag = headResponse.headers()["ETag"] ?: run {
            println("It does, but does not have an ETag!")
            exitProcess(-1)
        }
        println("It does! Starting download.")

        val fileSystemName = movie.substringBefore("/")

        val chunkRepo = if (noCache) {
            InMemoryChunkRepo()
        } else {
            DiskBackedChunkRepo(tempDir, fileSystemName, etag)
        }

        val endOfFileReached = AtomicBoolean()

        println()
        Observable.interval(10, TimeUnit.MILLISECONDS)
            .map { it.toInt() }
            .takeUntil { endOfFileReached.get() }
            .filter { chunkRepo.get(it) == null }
            .flatMap({ chunkNo ->
                print("\rDownloading chunk $chunkNo")
                val start = chunkNo * chunkSize
                val bytes = "bytes=${start}-${start + chunkSize - 1}"
                ArchiveAPI.Impl.download(movie, bytes, etag)
                    .doOnSuccess { if (it.code() == 416) endOfFileReached.set(true) }
                    .flatMapMaybe { handleResponse(chunkNo, it) }
                    .retry(2)
                    .toObservable()
            }, false, concurrentRequestMax)
            .blockingSubscribe { chunkRepo.set(it.number, it.data) }
        println()

        val out = File(System.getProperty("user.dir"), movie.substringAfterLast("/")).apply {
            val freshFile = createNewFile()
            require(freshFile) {
                "TODO: This should be checked before downloading"
            }
        }

        chunkRepo.chunks().forEach { out.appendBytes(it) }

        println("Done!")
    }

    private fun handleResponse(chunkNo: Int, response: Response<ByteArray>): Maybe<DownloadResult.Chunk> =
        when (val result = DownloadResult.from(response.code(), chunkNo, response.body())) {
            is DownloadResult.Empty -> Maybe.empty()
            is DownloadResult.Chunk -> Maybe.just(result)
            is DownloadResult.InvalidETag -> Maybe.error(
                IllegalStateException(
                    "ETag changed while downloading at chunk ${result.number}! All previous chunks are invalid."
                )
            )
            is DownloadResult.Unknown -> Maybe.error(
                IllegalStateException(
                    "Unknown response code ${response.code()}, message ${response.message()}"
                )
            )
        }
}

interface ChunkRepo {
    fun get(chunkNo: Int): ByteArray?
    fun set(chunkNo: Int, chunk: ByteArray)
    fun chunks(): Sequence<ByteArray>
}

class InMemoryChunkRepo : ChunkRepo {
    private val chunkMap: MutableMap<Int, ByteArray> = ConcurrentHashMap()

    override fun get(chunkNo: Int): ByteArray? = chunkMap[chunkNo]

    override fun set(chunkNo: Int, chunk: ByteArray) {
        chunkMap[chunkNo] = chunk
    }

    override fun chunks(): Sequence<ByteArray> {
        val snapshot = chunkMap.toMap()
        return sequence {
            for (i in 0..snapshot.size) {
                yield(snapshot.getValue(i))
            }
        }
    }
}

class DiskBackedChunkRepo(topLevelTmpDir: File, private val name: String, private val etag: String) : ChunkRepo {
    private val root: File = topLevelTmpDir.listFiles()!!

        .singleOrNull { it.name == name }
        ?: File(topLevelTmpDir.path + "/flying-saucer", name).apply { mkdir() }

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
                print("\rWriting chunk $lastChunk")
                yield(get(i)!!)
            }
        }
    }

    private fun chunkFile(chunkNo: Int) = File(root.path, "$etag.$chunkNo")
}

fun main(args: Array<String>) = Main.main(args)


