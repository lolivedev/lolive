package com.ho.lolive.di

import com.ho.lolive.BuildConfig
import com.ho.lolive.data.remote.GithubApiService
import com.ho.lolive.data.remote.LiveApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.zip.Inflater
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.GzipSource
import okio.InflaterSource
import okio.buffer
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val UPSTREAM_HOST = "api.hclyz.com"
    private const val GITHUB_HOST = "api.github.com"
    private const val HEADER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    private const val HEADER_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
    private const val HEADER_ACCEPT_ENCODING = "gzip, deflate"
    private const val HEADER_ACCEPT_LANGUAGE = "zh-CN,zh;q=0.9,en;q=0.8"
    private const val HEADER_SEC_CH_UA = "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\""
    private const val HEADER_SEC_CH_UA_MOBILE = "?0"
    private const val HEADER_SEC_CH_UA_PLATFORM = "\"Windows\""


    @Provides
    @Singleton
    @OptIn(ExperimentalSerializationApi::class)
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("User-Agent", HEADER_USER_AGENT)
                when (original.url.host) {
                    UPSTREAM_HOST -> {
                        builder
                            .header("Accept", HEADER_ACCEPT)
                            .header("Accept-Encoding", HEADER_ACCEPT_ENCODING)
                            .header("Accept-Language", HEADER_ACCEPT_LANGUAGE)
                            .header("Cache-Control", "no-cache")
                            .header("Pragma", "no-cache")
                            .header("Upgrade-Insecure-Requests", "1")
                            .header("Sec-CH-UA", HEADER_SEC_CH_UA)
                            .header("Sec-CH-UA-Mobile", HEADER_SEC_CH_UA_MOBILE)
                            .header("Sec-CH-UA-Platform", HEADER_SEC_CH_UA_PLATFORM)
                            .header("Sec-Fetch-Dest", "document")
                            .header("Sec-Fetch-Mode", "navigate")
                            .header("Sec-Fetch-Site", "none")
                            .header("Sec-Fetch-User", "?1")
                            // The upstream HTTP endpoint is unstable with keep-alive connections.
                            .header("Connection", "close")
                    }
                    GITHUB_HOST -> {
                        builder.header("Accept", "application/vnd.github+json")
                    }
                }
                val request = builder.build()
                chain.proceed(request)
            }
            .retryOnConnectionFailure(true)

        if (BuildConfig.ENABLE_HTTP_LOG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        builder.addInterceptor { chain ->
            val request = chain.request()
            val response = proceedWithDecompression(chain, request)
            response
        }

        return builder.build()
    }

    private fun proceedWithDecompression(
        chain: okhttp3.Interceptor.Chain,
        request: okhttp3.Request,
    ): Response {
        val response = chain.proceed(request)
        val body = response.body ?: return response
        val encoding = response.header("Content-Encoding")?.trim()?.lowercase() ?: return response

        val decompressedBytes = when (encoding) {
            "gzip" -> GzipSource(body.source()).buffer().use { it.readByteArray() }
            "deflate" -> InflaterSource(body.source(), Inflater()).buffer().use { it.readByteArray() }
            else -> return response
        }

        return response.newBuilder()
            .removeHeader("Content-Encoding")
            .removeHeader("Content-Length")
            .body(decompressedBytes.toResponseBody(body.contentType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideLiveApiService(retrofit: Retrofit): LiveApiService = retrofit.create(LiveApiService::class.java)

    @Provides
    @Singleton
    fun provideGithubApiService(retrofit: Retrofit): GithubApiService = retrofit.create(GithubApiService::class.java)
}
