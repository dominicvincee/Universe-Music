package com.universemusic

import android.app.Activity
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject
import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.models.YouTubeClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class InnerTubeBridge(private val activity: Activity, private val webView: WebView) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clientDelegate = lazy { HttpClient(OkHttp) }
    private val httpClient by clientDelegate
    private val innerTube by lazy { InnerTube(httpClient) }
    private val extractor by lazy {
        val store = RemotePlayerConfigStore(httpClient = httpClient, repository = PlayerConfigRepository.disabled())
        val cipher = YouTubeCipherService(httpClient, store)
        InnerTubeExtractor(
            configParser = YtConfigParserImpl(httpClient, innerTube, store, cipherService = cipher),
            cipherService = cipher,
            innerTube = innerTube
        )
    }
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private val progressTask = object : Runnable {
        override fun run() {
            val active = player ?: return
            try {
                if (active.isPlaying) {
                    val position = active.currentPosition
                    val duration = active.duration.coerceAtLeast(0)
                    webView.evaluateJavascript("window.onInnerTubeProgress && window.onInnerTubeProgress($position,$duration,true)", null)
                }
            } catch (_: Exception) { }
            handler.postDelayed(this, 1000)
        }
    }

    @JavascriptInterface
    fun searchMusic(query: String, callbackId: String) {
        if (query.isBlank()) {
            result(callbackId, JSONObject().put("ok", false).put("error", "Masukkan judul lagu atau nama artis."))
            return
        }
        scope.launch {
            try {
                val response = innerTube.search(YouTubeClient.WEB_REMIX, query = query.trim(), setLogin = false)
                val body = response.bodyAsText()
                if (response.status.value !in 200..299) error("YouTube Music merespons ${response.status.value}.")
                result(callbackId, JSONObject().put("ok", true).put("tracks", parseTracks(body)))
            } catch (error: Exception) {
                result(callbackId, JSONObject().put("ok", false).put("error", error.message ?: "Pencarian InnerTubeX gagal."))
            }
        }
    }

    @JavascriptInterface
    fun playMusic(videoId: String, title: String, callbackId: String) {
        if (!videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) {
            result(callbackId, JSONObject().put("ok", false).put("error", "ID lagu tidak valid."))
            return
        }
        scope.launch {
            try {
                val stream = extractor.extract(
                    videoId = videoId,
                    hints = ContentHints(wantVideo = false),
                    audioQuality = AudioQuality.HIGH
                ) ?: error("InnerTubeX tidak menemukan stream audio yang bisa diputar.")
                activity.runOnUiThread {
                    try {
                        releasePlayer()
                        val next = MediaPlayer()
                        player = next
                        next.setOnPreparedListener { prepared ->
                            prepared.start()
                            webView.evaluateJavascript(
                                "window.onInnerTubePlaybackStarted && window.onInnerTubePlaybackStarted(${JSONObject.quote(title)},${JSONObject.quote(videoId)})",
                                null
                            )
                            handler.removeCallbacks(progressTask)
                            handler.post(progressTask)
                            result(callbackId, JSONObject().put("ok", true))
                        }
                        next.setOnCompletionListener {
                            webView.evaluateJavascript("window.onInnerTubeEnded && window.onInnerTubeEnded()", null)
                        }
                        next.setOnErrorListener { _, what, extra ->
                            result(callbackId, JSONObject().put("ok", false).put("error", "Pemutar gagal (kode $what/$extra)."))
                            true
                        }
                        next.setDataSource(stream.audioUrl, stream.headers)
                        next.prepareAsync()
                    } catch (error: Exception) {
                        result(callbackId, JSONObject().put("ok", false).put("error", error.message ?: "Gagal menyiapkan pemutar."))
                    }
                }
            } catch (error: Exception) {
                result(callbackId, JSONObject().put("ok", false).put("error", error.message ?: "Ekstraksi stream InnerTubeX gagal."))
            }
        }
    }

    @JavascriptInterface
    fun pauseMusic() = activity.runOnUiThread { try { if (player?.isPlaying == true) player?.pause() } catch (_: Exception) { } }

    @JavascriptInterface
    fun resumeMusic() = activity.runOnUiThread { try { player?.let { if (!it.isPlaying) it.start() } } catch (_: Exception) { } }

    @JavascriptInterface
    fun seekMusic(positionMs: Int) = activity.runOnUiThread {
        try { player?.seekTo(positionMs.coerceAtLeast(0)) } catch (_: Exception) { }
    }

    @JavascriptInterface
    fun setVolume(volume: Float) = activity.runOnUiThread {
        try { player?.setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f)) } catch (_: Exception) { }
    }

    @JavascriptInterface
    fun stopMusic() = activity.runOnUiThread { releasePlayer() }

    fun release() {
        handler.removeCallbacks(progressTask)
        releasePlayer()
        scope.cancel()
        if (clientDelegate.isInitialized()) httpClient.close()
    }

    private fun releasePlayer() {
        handler.removeCallbacks(progressTask)
        try { player?.release() } catch (_: Exception) { }
        player = null
    }

    private fun result(callbackId: String, payload: JSONObject) {
        val safeId = JSONObject.quote(callbackId)
        val safePayload = JSONObject.quote(payload.toString())
        activity.runOnUiThread {
            webView.evaluateJavascript("window.onInnerTubeResult && window.onInnerTubeResult($safeId,$safePayload)", null)
        }
    }

    private fun parseTracks(raw: String): JSONArray {
        val root = JSONObject(raw)
        val tracks = JSONArray()
        val seen = mutableSetOf<String>()

        fun displayText(value: Any?): String? {
            when (value) {
                is JSONObject -> {
                    value.optString("simpleText").takeIf { it.isNotBlank() && it != "null" }?.let { return it }
                    value.optJSONArray("runs")?.let { runs ->
                        val joined = (0 until runs.length()).mapNotNull { runs.optJSONObject(it)?.optString("text")?.takeIf(String::isNotBlank) }.joinToString("")
                        if (joined.isNotBlank()) return joined
                    }
                    val keys = value.keys()
                    while (keys.hasNext()) displayText(value.opt(keys.next()))?.let { return it }
                }
                is JSONArray -> for (i in 0 until value.length()) displayText(value.opt(i))?.let { return it }
            }
            return null
        }

        fun findString(value: Any?, key: String): String? {
            when (value) {
                is JSONObject -> {
                    value.optString(key).takeIf { it.isNotBlank() && it != "null" }?.let { return it }
                    val keys = value.keys()
                    while (keys.hasNext()) findString(value.opt(keys.next()), key)?.let { return it }
                }
                is JSONArray -> for (i in 0 until value.length()) findString(value.opt(i), key)?.let { return it }
            }
            return null
        }

        fun thumbnail(value: Any?): String? {
            when (value) {
                is JSONObject -> {
                    val list = value.optJSONArray("thumbnails")
                    if (list != null) for (i in list.length() - 1 downTo 0) {
                        list.optJSONObject(i)?.optString("url")?.takeIf { it.startsWith("https://") }?.let { return it }
                    }
                    val keys = value.keys()
                    while (keys.hasNext()) thumbnail(value.opt(keys.next()))?.let { return it }
                }
                is JSONArray -> for (i in 0 until value.length()) thumbnail(value.opt(i))?.let { return it }
            }
            return null
        }

        fun visit(value: Any?) {
            when (value) {
                is JSONObject -> {
                    val renderer = value.optJSONObject("musicResponsiveListItemRenderer") ?: value.optJSONObject("videoRenderer")
                    if (renderer != null) {
                        val id = findString(renderer, "videoId")
                        if (!id.isNullOrBlank() && seen.add(id)) {
                            val columns = renderer.optJSONArray("flexColumns")
                            val title = if (columns != null && columns.length() > 0) displayText(columns.opt(0)) else null
                            val artist = if (columns != null && columns.length() > 1) displayText(columns.opt(1)) else null
                            tracks.put(JSONObject()
                                .put("id", id)
                                .put("title", title ?: displayText(renderer.opt("title")) ?: "YouTube Music")
                                .put("artist", artist ?: displayText(renderer.opt("shortBylineText")) ?: "YouTube Music")
                                .put("artwork", thumbnail(renderer) ?: ""))
                        }
                    }
                    val keys = value.keys()
                    while (keys.hasNext()) visit(value.opt(keys.next()))
                }
                is JSONArray -> for (i in 0 until value.length()) visit(value.opt(i))
            }
        }
        visit(root)
        return tracks
    }
}
