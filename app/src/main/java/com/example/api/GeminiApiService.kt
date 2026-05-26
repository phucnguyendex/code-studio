package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

class RetryInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var tryCount = 0
        val maxLimit = 5 // retry up to 5 times for 429

        while (!response.isSuccessful && response.code == 429 && tryCount < maxLimit) {
            tryCount++
            response.close() // Close the previous response body before retrying
            
            // Exponential backoff: 2s, 4s, 8s, 16s, 32s
            try {
                Thread.sleep((1000 * Math.pow(2.0, tryCount.toDouble())).toLong())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            
            response = chain.proceed(request)
        }
        return response
    }
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(RetryInterceptor())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun generateContent(
        prompt: String,
        systemPrompt: String? = null,
        model: String = "gemini-1.5-flash",
        customApiKey: String? = null,
        isViet: Boolean = true
    ): String {
        val apiKey = if (!customApiKey.isNullOrEmpty()) customApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            return if (isViet) {
                "VUI LÒNG CẤU HÌNH API KEY GEMINI TRONG CÀI ĐẶT CỦA CODE STUDIO HOẶC TRONG BẢNG SECRETS!"
            } else {
                "PLEASE CONFIGURE GEMINI API KEY IN CODE STUDIO SETTINGS OR SECRETS PANEL!"
            }
        }

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            systemInstruction = systemPrompt?.let {
                GeminiContent(parts = listOf(GeminiPart(text = it)))
            }
        )

        return try {
            val response = service.generateContent(model, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: if (isViet) "Không nhận được phản hồi từ mô hình AI." else "No response received from the AI model."
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                403 -> if (isViet) {
                    "Lỗi 403 (Forbidden): API Key không hợp lệ, chưa được kích hoạt dịch vụ Google Generative Language API, hoặc bị chặn theo khu vực.\nVui lòng kiểm tra lại cấu hình API key của bạn."
                } else {
                    "Error 403 (Forbidden): Invalid API Key, or Google Generative Language API service is not enabled, or region restricted.\nPlease verify your API key configuration."
                }
                429 -> if (isViet) {
                    "Lỗi 429 (Too Many Requests): Hệ thống AI đang quá tải hoặc API Key đã vượt quá giới hạn quota miễn phí.\nVui lòng đợi vài phút rồi thử lại, hoặc thay đổi API Key trong phần Cài đặt."
                } else {
                    "Error 429 (Too Many Requests): AI system is overloaded or API Key has exceeded its free quota limit.\nPlease wait a few minutes and try again, or change the API Key in Settings."
                }
                else -> if (isViet) {
                    "Lỗi kết nối Gemini API (HTTP ${e.code()}): ${e.message}"
                } else {
                    "Gemini API connection error (HTTP ${e.code()}): ${e.message}"
                }
            }
        } catch (e: Exception) {
            if (isViet) {
                "Lỗi kết nối Gemini API: ${e.message}"
            } else {
                "Gemini API connection error: ${e.message}"
            }
        }
    }
}
