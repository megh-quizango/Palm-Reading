package app.palm.core.network

import app.palm.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface HealthResult {
    data object Ok : HealthResult
    data class Failed(val cause: String) : HealthResult
}

class HealthClient @Inject constructor(private val http: OkHttpClient) {
    suspend fun check(): HealthResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(BuildConfig.BASE_URL + "/healthz").get().build()
            http.newCall(request).execute().use { response ->
                if (response.code != 200) return@use HealthResult.Failed("HTTP ${response.code}")
                val status = Json.parseToJsonElement(response.body.string())
                    .jsonObject["status"]?.jsonPrimitive?.content
                if (status == "ok") HealthResult.Ok else HealthResult.Failed("status=$status")
            }
        } catch (e: Exception) {
            HealthResult.Failed(e.javaClass.simpleName) // plan 01-03 maps causes to hints
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
}
