package com.ingredientchecker.app.data

import com.ingredientchecker.app.BuildConfig
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

interface IngredientApi {
    @GET("v1/allergens")
    suspend fun getAllergens(): AllergensResponse

    @Multipart
    @POST("v1/scan")
    suspend fun scan(
        @Part image: MultipartBody.Part,
        @Part("allergens") allergens: RequestBody,
        @Part("vegan") vegan: RequestBody,
        @Part("vegetarian") vegetarian: RequestBody,
        @Part("extra_avoid") extraAvoid: RequestBody,
    ): ScanResponse
}

object ApiClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    val api: IngredientApi = Retrofit.Builder()
        .baseUrl(ensureTrailingSlash(BuildConfig.API_BASE_URL))
        .client(http)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(IngredientApi::class.java)

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
