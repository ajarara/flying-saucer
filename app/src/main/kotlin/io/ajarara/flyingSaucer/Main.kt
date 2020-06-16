package io.ajarara.flyingSaucer

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.michaelbull.result.*
import io.ajarara.flyingSaucer.download.DownloadResult
import io.ajarara.flyingSaucer.download.HeadResponse
import io.ajarara.flyingSaucer.download.Headers
import io.ajarara.flyingSaucer.validation.ArchiveUrlValidationError
import io.ajarara.flyingSaucer.validation.parseMovie
import io.reactivex.Maybe
import io.reactivex.Observable
import retrofit2.Response
import java.io.File
import java.lang.IllegalStateException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

object Main : CliktCommand() {

    private val tempDir = File(System.getProperty("java.io.tmpdir"))

    private val archiveUrl: String by option(help = "Mp4 to download from archive.org")
        .default("https://archive.org/download/Plan_9_from_Outer_Space_1959/Plan_9_from_Outer_Space_1959_512kb.mp4")

    private val concurrentRequestMax: Int by option(help = "Number of requests to run simultaneously")
        .int()
        .default(4)

    private val noCache: Boolean by option(help = "Do not cache chunks on disk in between runs")
        .flag()
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
        val fileSystemName = movie.substringBefore("/")
        val out = File(System.getProperty("user.dir"), movie.substringAfterLast("/")).apply {
            if (exists()) {
                println("File $name already exists! Aborting.")
                exitProcess(-1)
            }
        }

        print("Checking if the download '$movie' exists: ")
        val headResponse = ArchiveAPI.Impl.check(movie)
            .map { HeadResponse.of(it.code(), it.headers().toMultimap()) }
            .blockingGet()
        val etag = handleHeadResponse(headResponse)

        val chunkRepo = if (noCache) {
            InMemoryChunkRepo()
        } else {
            val projectDir = File(tempDir, "io.ajarara.flyingSaucer").apply { mkdir() }
            DiskBackedChunkRepo(projectDir, fileSystemName, etag)
        }

        println()
        val endOfFileReached = AtomicBoolean()
        Observable.range(0, Int.MAX_VALUE)
            .takeUntil { endOfFileReached.get() }
            .filter { chunkRepo.get(it) == null }
            .flatMap({ chunkNo ->
                val bytes = Headers.bytesOf(chunkNo, chunkSize)

                ArchiveAPI.Impl.download(movie, bytes, etag.etag)
                    .doOnSuccess { if (it.code() == 416) endOfFileReached.set(true) }
                    .flatMapMaybe { handleResponse(chunkNo, it) }
                    .retry(2)
                    .toObservable()
            }, false, concurrentRequestMax)
            .blockingSubscribe { chunkRepo.set(it.number, it.data) }
        println()

        out.apply {
            createNewFile()
            chunkRepo.chunks().forEach(::appendBytes)
        }

        println("Done!")
    }

    private fun handleHeadResponse(headResponse: HeadResponse): HeadResponse.ETag =
        when (headResponse) {
            is HeadResponse.Error -> {
                when (headResponse) {
                    is HeadResponse.Error.NotOk -> println(
                        "Got a non-200 response from the check: ${headResponse.code}"
                    )
                    is HeadResponse.Error.NoETag -> println(
                        "Did not get any ETag from the check!"
                    )
                    is HeadResponse.Error.MultipleETags -> println(
                        "Multiple ETags returned, ambiguous: ${headResponse.etags}"
                    )
                }
                exitProcess(-1)
            }
            is HeadResponse.ETag -> {
                println("It does! Downloading.")
                headResponse
            }
        }

    private fun handleResponse(chunkNo: Int, response: Response<ByteArray>): Maybe<DownloadResult.Chunk> =
        when (val result = DownloadResult.from(response.code(), chunkNo, response.body())) {
            is DownloadResult.Empty -> Maybe.empty()
            is DownloadResult.Chunk -> {
                print("\rDownloading chunk $chunkNo")
                Maybe.just(result)
            }
            is DownloadResult.InvalidETag -> Maybe.error(
                IllegalStateException(
                    "ETag changed while downloading at chunk $chunkNo! All previous chunks are invalid."
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

fun main(args: Array<String>) = Main.main(args)


