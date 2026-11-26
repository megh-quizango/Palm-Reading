package app.palm.core.network

import app.palm.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.EOFException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.UnknownServiceException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
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
                val status = runCatching {
                    Json.parseToJsonElement(response.body.string()).jsonObject["status"]?.jsonPrimitive?.content
                }.getOrNull()
                if (status == "ok") HealthResult.Ok else HealthResult.Failed("Unexpected response")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            HealthResult.Failed(causeOf(e))
        }
    }
}

/** Turns a failed probe into a hint the developer can act on from the phone (Pitfall 10). */
internal fun causeOf(e: Throwable): String = when {
    e is ConnectException -> "Connection refused: run tools/dev-reverse.sh and tools/run-backend.sh"
    e is SocketTimeoutException -> "Timed out: is tools/run-backend.sh running?"
    e is UnknownServiceException && e.message.orEmpty().contains("CLEARTEXT") ->
        "Plain HTTP blocked: debug network config missing"
    e is UnknownHostException -> "Host not found"
    // adb reverse rule present, nothing listening on the PC: adbd accepts, then closes the socket.
    e is EOFException || e.cause is EOFException || e is SocketException ||
        e.message.orEmpty().contains("unexpected end of stream") -> "No response: start tools/run-backend.sh"
    else -> "Network error: ${e.javaClass.simpleName}"
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
