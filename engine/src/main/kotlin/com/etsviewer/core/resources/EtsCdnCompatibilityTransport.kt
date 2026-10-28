package com.etsviewer.core.resources

import com.etsviewer.core.service.ProtocolFailure
import com.etsviewer.core.service.standardHttpClient
import com.etsviewer.core.service.useResponse
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * ZIP-download transport only. The ETS CDN serves a certificate for a different hostname.
 * Its private client deliberately waives chain/identity verification for the exact CDN host.
 * Never use this component for API credentials, session tokens, or signed API requests.
 * Both app variants use this same implementation; no global TLS defaults are changed.
 */
class EtsCdnCompatibilityTransport(strictClient: OkHttpClient = standardHttpClient()) {
    private val strict = strictClient.newBuilder().followRedirects(false).followSslRedirects(false).build()
    // Build from scratch: no API interceptors, cookies, authenticator, certificate pins or pool.
    private val cdn = run {
        val trust = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) =
                throw java.security.cert.CertificateException("Client certificates are not supported")
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        }
        val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), SecureRandom()) }
        OkHttpClient.Builder().sslSocketFactory(context.socketFactory, trust)
            .hostnameVerifier { host, _ -> host == CDN_HOST }
            .dns(strictClient.dns).proxy(strictClient.proxy).proxySelector(strictClient.proxySelector)
            .connectTimeout(strictClient.connectTimeoutMillis.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(strictClient.readTimeoutMillis.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .callTimeout(strictClient.callTimeoutMillis.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .followRedirects(false).followSslRedirects(false).build()
    }

    suspend fun <T> useDownloadResponse(url: String, consume: suspend (Response) -> T): T {
        var destination = url.toHttpUrl()
        var redirects = 0
        while (true) {
            if (destination.username.isNotEmpty() || destination.password.isNotEmpty())
                throw ProtocolFailure("Resource URL must not contain credentials")
            // Check the actual, canonical request destination on every hop, before choosing TLS.
            val client = if (destination.scheme == "https" && destination.host == CDN_HOST) cdn else strict
            val request = Request.Builder().url(destination)
                .header("User-Agent", "libcurl-agent/1.0").header("Accept", "*/*").get().build()
            val hop: Hop<T> = client.newCall(request).useResponse { response ->
                val location = response.header("Location")
                if (response.code in REDIRECT_CODES && location != null) {
                    if (redirects++ >= MAX_REDIRECTS) throw ProtocolFailure("Too many resource redirects")
                    val next = destination.resolve(location) ?: throw ProtocolFailure("Invalid resource redirect")
                    if (destination.scheme == "https" && next.scheme != "https")
                        throw ProtocolFailure("Resource redirect cannot downgrade HTTPS")
                    Hop.Next(next)
                } else Hop.Done(consume(response))
            }
            when (hop) {
                is Hop.Next -> destination = hop.destination
                is Hop.Done -> return hop.value
            }
        }
    }

    private sealed interface Hop<out T> {
        data class Next(val destination: HttpUrl) : Hop<Nothing>
        data class Done<T>(val value: T) : Hop<T>
    }
    private companion object {
        const val CDN_HOST = "cdn.subject.ets100.com"
        const val MAX_REDIRECTS = 20
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
