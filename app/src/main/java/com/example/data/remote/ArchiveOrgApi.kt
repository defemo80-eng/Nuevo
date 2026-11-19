package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ArchiveSearchResponse(
    val response: ArchiveSearchBody? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveSearchBody(
    val numFound: Int = 0,
    val docs: List<ArchiveDoc> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArchiveDoc(
    val identifier: String = "",
    val title: String? = null,
    val description: Any? = null,
    val year: Any? = null,
    val creator: Any? = null,
    val subject: Any? = null,
    val language: Any? = null,
    val downloads: Int? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveMetadataResponse(
    val server: String? = null,
    val dir: String? = null,
    val files: List<ArchiveFileItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArchiveFileItem(
    val name: String = "",
    val format: String? = null,
    val size: String? = null,
    val length: String? = null
)

@JsonClass(generateAdapter = true)
data class CloudCodeRedeemRequest(
    val code: String,
    val userId: String,
    val email: String
)

@JsonClass(generateAdapter = true)
data class CloudCodeRedeemResponse(
    val success: Boolean = false,
    val message: String = "",
    val activatedAt: Long? = null,
    val expiresAt: Long? = null,
    val durationDays: Int = 30
)

interface ArchiveOrgApi {
    @GET("advancedsearch.php")
    suspend fun searchPublicDomainMedia(
        @Query("q") query: String,
        @Query("fl[]") fields: List<String> = listOf(
            "identifier", "title", "description", "year", "creator", "subject", "language", "downloads"
        ),
        @Query("sort[]") sort: String = "downloads desc",
        @Query("rows") rows: Int = 12,
        @Query("page") page: Int = 1,
        @Query("output") output: String = "json"
    ): ArchiveSearchResponse

    @GET("metadata/{identifier}")
    suspend fun getMetadata(@Path("identifier") identifier: String): ArchiveMetadataResponse

    companion object {
        private const val BASE_URL = "https://archive.org/"

        fun create(): ArchiveOrgApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(18, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(ArchiveOrgApi::class.java)
        }
    }
}
