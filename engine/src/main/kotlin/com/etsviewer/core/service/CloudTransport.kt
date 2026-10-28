package com.etsviewer.core.service

import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

fun standardHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).callTimeout(60, TimeUnit.SECONDS)
    .followSslRedirects(false).build()

suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _, value, _ -> value.close() }
        }
    })
}

suspend fun <T> Call.useResponse(consume: suspend (Response) -> T): T = coroutineScope {
    val request = this@useResponse
    // Keep cancellation connected to the socket until its body has been consumed.
    val cancellation = launch(start = CoroutineStart.UNDISPATCHED) {
        try { awaitCancellation() } finally { request.cancel() }
    }
    try { request.awaitResponse().use { consume(it) } }
    catch (error: Exception) {
        currentCoroutineContext().ensureActive()
        throw error
    }
    finally { cancellation.cancel() }
}

class CloudTransport(
    private val codec: EtsPayloadCodec,
    private val client: OkHttpClient = standardHttpClient(),
    private val baseUrl: String = "https://api.ets100.com",
) {
    suspend fun post(route: String, parameters: JsonObject): EtsReply = withContext(Dispatchers.IO) {
        val body = codec.encode(route, parameters)
        val request = Request.Builder().url(baseUrl.trimEnd('/') + "/" + route)
            .header("User-Agent", "libcurl-agent/1.0").header("Accept", "*/*")
            .post(body.toByteArray(Charsets.UTF_8).toRequestBody("application/x-www-form-urlencoded".toMediaType())).build()
        client.newCall(request).useResponse { response ->
            if (!response.isSuccessful) throw HttpFailure(response.code)
            EtsReplyParser.parse(response.body?.string() ?: throw ProtocolFailure("Empty ETS100 response"))
        }
    }
}
