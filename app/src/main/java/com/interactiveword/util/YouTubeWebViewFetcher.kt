package com.interactiveword.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class YouTubeWebViewFetcher(private val context: Context) {

    private val videoIdRegexes = listOf(
        Regex("(?:v=)([a-zA-Z0-9_-]{11})"),
        Regex("(?:youtu\\.be/)([a-zA-Z0-9_-]{11})"),
        Regex("(?:shorts/)([a-zA-Z0-9_-]{11})")
    )

    fun extractVideoId(url: String): String? {
        videoIdRegexes.forEach { it.find(url)?.let { m -> return m.groupValues[1] } }
        return null
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun fetchTranscript(url: String): String {
        val videoId = extractVideoId(url)
            ?: throw Exception("유효한 YouTube URL이 아닙니다.")
        android.util.Log.d("YTWebView", "videoId=$videoId")

        // 1단계: WebView로 YouTube 페이지 로드 → 서명된 captionUrl 추출 + 쿠키 세팅
        val captionUrl = extractCaptionUrl(videoId)
        android.util.Log.d("YTWebView", "captionUrl=$captionUrl")

        // 2단계: WebView가 심어준 쿠키(JS-set 포함)를 CookieManager에서 읽어 OkHttp로 요청
        val cookies = android.webkit.CookieManager.getInstance()
            .getCookie("https://www.youtube.com") ?: ""
        android.util.Log.d("YTWebView", "cookies len=${cookies.length}")

        val rawBody = withContext(Dispatchers.IO) { fetchWithCookies(captionUrl, cookies) }
        android.util.Log.d("YTWebView", "rawBody len=${rawBody.length} head=${rawBody.take(100)}")

        return parseCaption(rawBody)
    }

    // 1단계: YouTube 시청 페이지에서 한국어 캡션 URL 추출
    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun extractCaptionUrl(videoId: String): String =
        suspendCancellableCoroutine { cont ->
            Handler(Looper.getMainLooper()).post {
                val webView = WebView(context)
                webView.settings.javaScriptEnabled = true
                webView.settings.userAgentString =
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

                val iface = object : Any() {
                    @JavascriptInterface
                    fun onUrl(url: String) {
                        android.util.Log.d("YTWebView", "captionUrl extracted: ${url.take(80)}")
                        if (!cont.isCompleted) cont.resume(url)
                        webView.post { webView.destroy() }
                    }
                    @JavascriptInterface
                    fun onError(msg: String) {
                        android.util.Log.d("YTWebView", "extractCaptionUrl error: $msg")
                        if (!cont.isCompleted) cont.resumeWithException(Exception(msg))
                        webView.post { webView.destroy() }
                    }
                }
                webView.addJavascriptInterface(iface, "YTAndroid")

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, pageUrl: String) {
                        if (!pageUrl.startsWith("https://www.youtube.com")) return
                        view.evaluateJavascript("""
                            (function() {
                                try {
                                    var r = window.ytInitialPlayerResponse;
                                    if (!r) { YTAndroid.onError('ytInitialPlayerResponse 없음'); return; }
                                    var tracks = r.captions
                                        && r.captions.playerCaptionsTracklistRenderer
                                        && r.captions.playerCaptionsTracklistRenderer.captionTracks;
                                    if (!tracks || !tracks.length) { YTAndroid.onError('자막 없음'); return; }

                                    // manual 우선, 없으면 asr
                                    var koTrack = null;
                                    for (var i = 0; i < tracks.length; i++) {
                                        var t = tracks[i], lc = t.languageCode;
                                        if ((lc==='ko'||lc==='ko-KR') && (!t.kind||t.kind==='')) { koTrack=t; break; }
                                    }
                                    if (!koTrack) for (var i = 0; i < tracks.length; i++) {
                                        var t = tracks[i], lc = t.languageCode;
                                        if (lc==='ko'||lc==='ko-KR') { koTrack=t; break; }
                                    }
                                    if (!koTrack) { YTAndroid.onError('한국어 자막 없음'); return; }

                                    // fmt=json3 고정
                                    var rawUrl = koTrack.baseUrl.replace(/&fmt=[^&]*/g,'') + '&fmt=json3';
                                    YTAndroid.onUrl(rawUrl);
                                } catch(e) { YTAndroid.onError('JS 오류: ' + e); }
                            })();
                        """.trimIndent(), null)
                    }
                }
                webView.loadUrl("https://www.youtube.com/watch?v=$videoId")
                cont.invokeOnCancellation { webView.post { webView.destroy() } }
            }
        }

    // 2단계: WebView가 세팅한 쿠키 + OkHttp로 captionUrl 요청
    private fun fetchWithCookies(captionUrl: String, cookies: String): String {
        val client = OkHttpClient.Builder().followRedirects(true).build()
        val request = Request.Builder()
            .url(captionUrl)
            .header("Cookie", cookies)
            .header("Referer", "https://www.youtube.com/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "application/json, text/javascript, */*; q=0.01")
            .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8")
            .header("Origin", "https://www.youtube.com")
            .build()
        return client.newCall(request).execute().use { response ->
            val type = response.header("Content-Type") ?: ""
            val body = response.body?.string() ?: ""
            android.util.Log.d("YTWebView", "OkHttp code=${response.code} type=$type len=${body.length}")
            body
        }
    }

    private fun parseCaption(body: String): String {
        if (body.isBlank()) throw Exception("자막 내용을 파싱할 수 없습니다.")
        val text = when {
            body.trimStart().startsWith("{") -> parseCaptionJson3(body)
            body.trimStart().startsWith("<") -> parseCaptionXml(body)
            else -> throw Exception("알 수 없는 자막 포맷 (len=${body.length})")
        }
        if (text.isBlank()) throw Exception("자막 내용을 파싱할 수 없습니다.")
        return text
    }

    private fun parseCaptionJson3(json: String): String =
        Regex(""""utf8":"([^"]+)"""")
            .findAll(json)
            .map { it.groupValues[1].replace("\\n", " ").trim() }
            .filter { it.isNotEmpty() && it != "\n" }
            .joinToString(" ")

    private fun parseCaptionXml(xml: String): String =
        Regex("<text[^>]*>([^<]+)</text>")
            .findAll(xml)
            .map { it.groupValues[1] }
            .map {
                it.replace("&#39;", "'").replace("&amp;", "&")
                    .replace("&quot;", "\"").replace("&lt;", "<")
                    .replace("&gt;", ">").replace("<br/>", " ").trim()
            }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
}
