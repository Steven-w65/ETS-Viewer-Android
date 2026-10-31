package com.etsviewer.android.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebMessageCompat
import com.etsviewer.core.identity.ChallengeCoordinator
import com.etsviewer.core.identity.ChallengeProof
import com.etsviewer.core.identity.ChallengeEvent
import com.etsviewer.core.feedback.*
import com.etsviewer.android.ui.VerificationFrame
import kotlinx.serialization.json.*
import java.net.URI

private fun trustedPage(url: String?): Boolean = runCatching {
    val uri = URI(url ?: "")
    uri.scheme == "https" && uri.port in setOf(-1, 443) && uri.host in setOf("pass.changyan.com", "www.ets100.com")
}.getOrDefault(false)

// Observes the service response without changing requests, page behavior, or challenge results.
internal const val verificationResultScript = "window.__etsCheckLoginResponse || null"
internal val captureScript = """
    (function() {
      if (window.__etsCaptureInstalled) return;
      window.__etsCaptureInstalled = true;
      function relevant(url) {
        try { var u = new URL(url, location.href);
          return u.protocol === 'https:' &&
            (u.hostname === 'pass.changyan.com' || u.hostname === 'www.ets100.com') &&
            u.pathname === '/login/checkLogin';
        } catch(e) { return false; }
      }
      function capture(text, status) {
        try {
          var raw = typeof text === 'string' ? text : JSON.stringify(text);
          var event = JSON.stringify({status:status, responseBody:raw});
          if (window.etsVerificationObserver) window.etsVerificationObserver.postMessage(event);
          else window.__etsCheckLoginResponse = event;
        } catch(e) {}
      }
      var open = XMLHttpRequest.prototype.open;
      XMLHttpRequest.prototype.open = function(method, url) {
        if (relevant(url)) this.addEventListener('load', function() {
          try { capture(this.responseType === 'json' ? this.response : this.responseText, this.status); } catch(e) {}
        });
        return open.apply(this, arguments);
      };
      if (window.fetch) {
        var fetch = window.fetch;
        window.fetch = function(input) {
          var url = typeof input === 'string' ? input : input.url;
          return fetch.apply(this, arguments).then(function(response) {
            if (relevant(url)) response.clone().text().then(function(text){capture(text, response.status);}).catch(function(){});
            return response;
          });
        };
      }
    })();
""".trimIndent()

internal fun attachVerificationCapture(view: WebView, onResponse: (String) -> Unit) {
    val origins = setOf("https://pass.changyan.com", "https://www.ets100.com")
    if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
        WebViewCompat.addWebMessageListener(view, "etsVerificationObserver", origins) { browser, message, origin, _, _ ->
            if (trustedPage(browser.url) && trustedPage(origin.toString()) && message.type == WebMessageCompat.TYPE_STRING)
                message.data?.let(onResponse)
        }
    }
    if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT))
        WebViewCompat.addDocumentStartJavaScript(view, captureScript, origins)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChallengeScreen(
    onComplete: (ChallengeProof) -> Unit, onCancel: () -> Unit,
    onRejected: (ChallengeEvent.Rejected) -> Unit, onRetry: () -> Unit,
    failure: AppFailure? = null, expired: Boolean = false,
) {
    var browser by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<AppFailure?>(null) }
    var delivered by remember { mutableStateOf(false) }
    val complete by rememberUpdatedState(onComplete)
    val rejected by rememberUpdatedState(onRejected)
    val receive: (String) -> Unit = { raw ->
        if (!delivered) when (val event = ChallengeCoordinator.decodeEvent(raw)) {
            is ChallengeEvent.Verified -> { delivered = true; complete(event.proof) }
            is ChallengeEvent.Rejected -> { error = event.failure; rejected(event) }
            null -> Unit
        }
    }
    VerificationFrame(loading, error ?: failure, expired, onCancel, onRetry) { browserModifier ->
        AndroidView(factory = { context ->
            WebView(context).apply {
                browser = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                attachVerificationCapture(this, receive)
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) { loading = true; error = null }
                    override fun onPageFinished(view: WebView, url: String?) {
                        loading = false
                        if (trustedPage(url)) view.evaluateJavascript(captureScript, null)
                    }
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                        if (request.isForMainFrame) { loading = false; error = AppFailure(FeedbackOperation.VERIFICATION, if (failure.errorCode in setOf(WebViewClient.ERROR_HOST_LOOKUP, WebViewClient.ERROR_CONNECT, WebViewClient.ERROR_TIMEOUT)) FailureKind.NETWORK else FailureKind.UNKNOWN) }
                    }
                    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                        if (request.isForMainFrame) { loading = false; error = AppFailure(FeedbackOperation.VERIFICATION, httpStatus = response.statusCode.takeIf { it in 100..599 }) }
                    }
                    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, failure: android.net.http.SslError) {
                        handler.cancel(); loading = false; error = AppFailure(FeedbackOperation.VERIFICATION, FailureKind.NETWORK, setOf(DiagnosticKind.TLS))
                    }
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (request.url.scheme == "https") return false
                        error = AppFailure(FeedbackOperation.VERIFICATION); return true
                    }
                }
                loadUrl(ChallengeCoordinator.PAGE_URL)
            }
        }, modifier = browserModifier)
    }
    DisposableEffect(browser) {
        val view = browser
        val handler = Handler(Looper.getMainLooper())
        var completed = false
        val poll = object : Runnable {
            override fun run() {
                if (!completed && !delivered && view != null && trustedPage(view.url)) {
                    view.evaluateJavascript("(function(){var v=" + verificationResultScript + ";window.__etsCheckLoginResponse=null;return v;})()") { result ->
                        val response = runCatching { Json.parseToJsonElement(result).jsonPrimitive.contentOrNull }.getOrNull()
                        if (!completed && response != null) receive(response)
                    }
                }
                if (!completed && !delivered) handler.postDelayed(this, 500)
            }
        }
        if (view != null) handler.post(poll)
        onDispose { completed = true; handler.removeCallbacks(poll); view?.stopLoading(); view?.destroy() }
    }
}
