package com.interactiveword.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object YouTubeTranscriptFetcher {

    private val cookieJar = object : okhttp3.CookieJar {
        private val store = java.util.concurrent.ConcurrentHashMap<String, MutableList<okhttp3.Cookie>>()
        override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
            store.getOrPut(url.host) { mutableListOf() }.also { list ->
                cookies.forEach { new -> list.removeAll { it.name == new.name }; list.add(new) }
            }
        }
        override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> =
            store[url.host] ?: emptyList()
    }

    private val client = OkHttpClient.Builder().cookieJar(cookieJar).build()

    fun extractVideoId(url: String): String? {
        listOf(
            Regex("(?:v=)([a-zA-Z0-9_-]{11})"),
            Regex("(?:youtu\\.be/)([a-zA-Z0-9_-]{11})"),
            Regex("(?:shorts/)([a-zA-Z0-9_-]{11})")
        ).forEach { it.find(url)?.let { m -> return m.groupValues[1] } }
        return null
    }

    suspend fun fetchTranscript(url: String): String = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(url)
            ?: throw Exception("유효한 YouTube URL이 아닙니다.")

        android.util.Log.d("YTFetcher", "videoId=$videoId")

        // 1차: 서명 없는 기본 timedtext URL (가장 단순한 방법)
        val simpleBody = trySimpleTimedText(videoId)
        if (simpleBody != null) return@withContext simpleBody

        // 2차: captionTracks URL 추출 후 fetch
        val tracksJson = tryHtmlPage(videoId)
            ?: tryInnerTubeTv(videoId)
            ?: throw Exception("이 영상에 자막이 없습니다.")

        android.util.Log.d("YTFetcher", "tracksJson(300): ${tracksJson.take(300)}")

        val captionUrl = findCaptionUrl(tracksJson, "ko")
            ?: findCaptionUrl(tracksJson, "ko-KR")
        android.util.Log.d("YTFetcher", "captionUrl=$captionUrl")

        captionUrl ?: throw Exception("이 영상에 한국어 자막이 없습니다.\n(자동 생성 자막도 없는 영상입니다)")

        val (code, type, body) = fetchCaptionBody(captionUrl)
        android.util.Log.d("YTFetcher", "caption HTTP $code | type=$type | len=${body.length}")
        android.util.Log.d("YTFetcher", "caption body(300): ${body.take(300)}")

        parseCaption(body)
    }

    // 서명 없는 기본 timedtext URL 시도 (공개 영상 ASR에 동작 여부 확인)
    private fun trySimpleTimedText(videoId: String): String? {
        for (langCode in listOf("ko", "ko-KR")) {
            for (fmt in listOf("json3", "srv3", "")) {
                val url = "https://www.youtube.com/api/timedtext?v=$videoId&lang=$langCode" +
                        if (fmt.isNotEmpty()) "&fmt=$fmt" else ""
                try {
                    val (code, type, body) = fetchCaptionBody(url)
                    android.util.Log.d("YTFetcher", "simple[$langCode,$fmt]: code=$code type=$type len=${body.length}")
                    if (body.isNotBlank() && !type.startsWith("text/html")) {
                        val text = parseCaption(body)
                        if (text.isNotBlank()) return text
                    }
                } catch (_: Exception) {}
            }
        }
        return null
    }

    private fun parseCaption(body: String): String {
        if (body.isBlank()) throw Exception("자막 내용을 파싱할 수 없습니다.")
        return when {
            body.trimStart().startsWith("{") -> parseCaptionJson3(body)
            body.trimStart().startsWith("<") -> parseCaptionXml(body)
            else -> throw Exception("알 수 없는 자막 포맷 (len=${body.length})")
        }.also { if (it.isBlank()) throw Exception("자막 내용을 파싱할 수 없습니다.") }
    }

    // youtube-transcript-api v1.2.4가 실제로 사용하는 클라이언트
    private fun tryInnerTubeTv(videoId: String): String? {
        return try {
            val body = """{"videoId":"$videoId","context":{"client":{"clientName":"TVHTML5_SIMPLY_EMBEDDED_PLAYER","clientVersion":"2.0"},"thirdParty":{"embedUrl":"https://www.youtube.com/"}}}"""
            val json = client.newCall(
                Request.Builder()
                    .url("https://www.youtube.com/youtubei/v1/player")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .header("Content-Type", "application/json")
                    .build()
            ).execute().use { it.body?.string() ?: return null }

            android.util.Log.d("YTFetcher", "InnerTube TV(300): ${json.take(300)}")
            if (json.contains("\"error\"")) { android.util.Log.d("YTFetcher", "InnerTube TV error"); return null }
            extractArrayContent(json, "\"captionTracks\":[")
        } catch (e: Exception) {
            android.util.Log.d("YTFetcher", "InnerTube TV ex: ${e.message}")
            null
        }
    }

    private fun tryHtmlPage(videoId: String): String? {
        return try {
            val html = client.newCall(
                Request.Builder()
                    .url("https://www.youtube.com/watch?v=$videoId")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .build()
            ).execute().use { it.body?.string() ?: return null }

            android.util.Log.d("YTFetcher", "HTML hasCaptionTracks=${html.contains("captionTracks")}")
            extractArrayContent(html, "\"captionTracks\":[")
        } catch (e: Exception) {
            android.util.Log.d("YTFetcher", "HTML ex: ${e.message}")
            null
        }
    }

    // fmt=json3 명시 → 빈 응답 방지
    private fun fetchCaptionBody(captionUrl: String): Triple<Int, String, String> {
        val url = if (captionUrl.contains("fmt=")) captionUrl else "$captionUrl&fmt=json3"
        return client.newCall(
            Request.Builder()
                .url(url)
                .header("Referer", "https://www.youtube.com/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
        ).execute().use { Triple(it.code, it.header("Content-Type") ?: "", it.body?.string() ?: "") }
    }

    private fun extractArrayContent(json: String, marker: String): String? {
        val idx = json.indexOf(marker)
        if (idx == -1) return null
        val start = idx + marker.length - 1
        var depth = 0
        var end = -1
        for (i in start until json.length) {
            when (json[i]) {
                '[' -> depth++
                ']' -> { depth--; if (depth == 0) { end = i; break } }
            }
        }
        if (end == -1) return null
        return json.substring(start + 1, end)
    }

    private fun findCaptionUrl(tracksJson: String, langCode: String): String? {
        var searchFrom = 0
        while (true) {
            val langPos = tracksJson.indexOf("\"languageCode\":\"$langCode\"", searchFrom)
            if (langPos == -1) return null

            val objStart = findOuterBrace(tracksJson, langPos)
            if (objStart == -1) { searchFrom = langPos + 1; continue }

            var depth = 0
            var objEnd = -1
            for (i in objStart until tracksJson.length) {
                when (tracksJson[i]) {
                    '{' -> depth++
                    '}' -> { depth--; if (depth == 0) { objEnd = i; break } }
                }
            }
            if (objEnd == -1) return null

            val trackObj = tracksJson.substring(objStart, objEnd + 1)
            val match = Regex(""""baseUrl":"([^"]+)"""").find(trackObj)
            if (match != null) {
                return match.groupValues[1]
                    .replace("\\u0026", "&")
                    .replace("\\/", "/")
            }
            searchFrom = langPos + 1
        }
    }

    private fun findOuterBrace(json: String, pos: Int): Int {
        var depth = 0
        for (i in pos downTo 0) {
            when (json[i]) {
                '}' -> depth++
                '{' -> { if (depth == 0) return i; depth-- }
            }
        }
        return -1
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
                it.replace("&#39;", "'")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("<br/>", " ")
                    .trim()
            }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
}
