package io.ajarara.flyingSaucer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.reactivex.Completable
import io.reactivex.Single
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

    // https://archive.org/download/Plan_9_from_Outer_Space_1959/Plan_9_from_Outer_Space_1959_512kb.mp4
    @GET("download/{movie}")
    fun download(
        @Path("movie", encoded = true) path: String,
        @Header("Range") bytes: String
    ): Single<Response<ByteArray>>

    object Impl : ArchiveAPI by Retrofit.Builder()
        .baseUrl("https://archive.org/")
        .addConverterFactory(ByteArrayConverterFactory)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
        .create(ArchiveAPI::class.java) {

        private object ByteArrayConverterFactory : Converter.Factory() {
            override fun responseBodyConverter(
                type: Type,
                annotations: Array<Annotation>,
                retrofit: Retrofit
            ): Converter<ResponseBody, *>? {
                if (type == ByteArray::class.java) {
                    return ByteArrayConverter
                }
                return null
            }

            private object ByteArrayConverter : Converter<ResponseBody, ByteArray> {
                override fun convert(value: ResponseBody): ByteArray = value.bytes()
            }
        }
    }
}