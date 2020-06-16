package io.ajarara.flyingSaucer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.Path
import java.lang.reflect.Type
import kotlin.reflect.KClass

interface ArchiveAPI {

    @HEAD("download/{movie}")
    fun check(@Path("movie", encoded = true) path: String): Single<Response<Void>>

    @HEAD("download/{movie}")
    fun check(
        @Path("movie", encoded = true) path: String,
        @Header("Range") bytes: String
    ): Single<Response<Void>>

    @GET("download/{movie}")
    fun download(
        @Path("movie", encoded = true) path: String,
        @Header("Range") bytes: String,
        @Header("If-Exists") etag: String
    ): Single<Response<ByteArray>>

    @GET("download/{movie}")
    fun uncheckedDownload(
        @Path("movie", encoded = true) path: String,
        @Header("Range") bytes: String
    ): Single<Response<ByteArray>>

    object Impl : ArchiveAPI by Retrofit.Builder()
        .baseUrl("https://archive.org/")
        .addConverterFactory(ByteArrayConverterFactory)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.computation()))
        .build()
        .create(ArchiveAPI::class.java) {

        private object ByteArrayConverterFactory : Converter.Factory() {
            override fun responseBodyConverter(
                type: Type,
                annotations: Array<Annotation>,
                retrofit: Retrofit
            ): Converter<ResponseBody, *>? = when (type) {
                ByteArray::class.java -> ByteArrayConverter
                else -> null
            }

            private object ByteArrayConverter : Converter<ResponseBody, ByteArray> {
                override fun convert(value: ResponseBody): ByteArray = value.bytes()
            }
        }
    }
}