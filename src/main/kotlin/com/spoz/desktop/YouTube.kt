package com.spoz.desktop

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

data class Track(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnail: String?,
)

/**
 * The whole catalogue layer: YouTube Music's InnerTube API.
 *
 * Search uses the WEB_REMIX client (the one music.youtube.com itself uses) so the
 * results are music results, not general videos. Stream resolution uses the IOS
 * client, whose player response hands back a directly playable audio URL instead
 * of one wrapped in a signature cipher.
 */
object YouTube {
    private val http = HttpClient(OkHttp) { expectSuccess = true }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private const val ORIGIN = "https://music.youtube.com"
    private const val REMIX_NAME = "WEB_REMIX"
    private const val REMIX_ID = "67"
    private const val REMIX_VER = "1.20260114.03.00"
    private const val IOS_NAME = "IOS"
    private const val IOS_ID = "5"
    private const val IOS_VER = "21.03.1"

    private fun context(name: String, version: String) = buildJsonObject {
        putJsonObject("client") {
            put("clientName", name)
            put("clientVersion", version)
            put("hl", "en")
            put("gl", "US")
        }
        putJsonObject("user") { put("lockedSafetyMode", false) }
    }

    private suspend fun post(path: String, id: String, name: String, ver: String, payload: JsonObject): JsonElement =
        json.parseToJsonElement(
            http.post("$ORIGIN/youtubei/v1/$path?prettyPrint=false") {
                contentType(ContentType.Application.Json)
                header("X-Goog-Api-Format-Version", "1")
                header("X-YouTube-Client-Name", id)
                header("X-YouTube-Client-Version", ver)
                header("X-Origin", ORIGIN)
                header("Referer", "$ORIGIN/")
                setBody(payload.toString())
            }.bodyAsText(),
        )

    suspend fun search(query: String): List<Track> {
        val payload = buildJsonObject {
            put("context", context(REMIX_NAME, REMIX_VER))
            put("query", query)
        }
        val root = post("search", REMIX_ID, REMIX_NAME, REMIX_VER, payload)
        val found = LinkedHashMap<String, Track>()
        walk(root) { obj ->
            val r = obj["musicResponsiveListItemRenderer"]?.jsonObjectOrNull ?: return@walk
            val cols = r["flexColumns"]?.jsonArrayOrNull ?: return@walk
            val videoId = r["playlistItemData"]?.jsonObjectOrNull?.get("videoId")?.strOrNull
                ?: firstVideoId(r) ?: return@walk
            val title = cols.getOrNull(0)?.let { columnText(it) } ?: return@walk
            val artist = cols.getOrNull(1)?.let { columnText(it) }.orEmpty()
            val thumb = firstThumbnail(r)
            if (videoId.isNotBlank() && title.isNotBlank()) {
                found[videoId] = Track(videoId, title, artist, thumb)
            }
        }
        return found.values.toList()
    }

    /** The best audio-only URL for a video, or null if the response carried none. */
    suspend fun streamUrl(videoId: String): String? {
        val payload = buildJsonObject {
            put("context", context(IOS_NAME, IOS_VER))
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }
        val root = post("player", IOS_ID, IOS_NAME, IOS_VER, payload).jsonObjectOrNull ?: return null
        val formats = root["streamingData"]?.jsonObjectOrNull
            ?.get("adaptiveFormats")?.jsonArrayOrNull ?: return null

        fun mime(f: JsonObject) = f["mimeType"]?.strOrNull.orEmpty()
        fun url(f: JsonObject) = f["url"]?.strOrNull

        // Prefer an audio-only m4a: JavaFX Media plays AAC natively.
        val audio = formats.mapNotNull { it.jsonObjectOrNull }
            .filter { mime(it).startsWith("audio/") }
        val best = audio.filter { mime(it).contains("mp4") }.maxByOrNull { it["bitrate"]?.intOrNull ?: 0 }
            ?: audio.maxByOrNull { it["bitrate"]?.intOrNull ?: 0 }
        return best?.let { url(it) }
    }

    // ---- JSON digging helpers (kept deliberately loose: InnerTube's shape moves) ----

    private fun walk(e: JsonElement, visit: (JsonObject) -> Unit) {
        when (e) {
            is JsonObject -> { visit(e); e.values.forEach { walk(it, visit) } }
            is JsonArray -> e.forEach { walk(it, visit) }
            else -> {}
        }
    }

    private fun columnText(column: JsonElement): String {
        val runs = column.jsonObjectOrNull?.get("text")?.jsonObjectOrNull?.get("runs")?.jsonArrayOrNull
        if (runs != null) return runs.mapNotNull { it.jsonObjectOrNull?.get("text")?.strOrNull }.joinToString("").trim()
        return column.jsonObjectOrNull?.get("text")?.jsonObjectOrNull?.get("simpleText")?.strOrNull.orEmpty()
    }

    private fun firstVideoId(e: JsonElement): String? {
        var hit: String? = null
        walk(e) { o ->
            if (hit == null) {
                o["watchEndpoint"]?.jsonObjectOrNull?.get("videoId")?.strOrNull?.let { hit = it }
            }
        }
        return hit
    }

    private fun firstThumbnail(r: JsonObject): String? {
        var hit: String? = null
        walk(r) { o ->
            if (hit == null) {
                val thumbs = o["thumbnails"]?.jsonArrayOrNull
                hit = thumbs?.lastOrNull()?.jsonObjectOrNull?.get("url")?.strOrNull
            }
        }
        return hit
    }
}

private val JsonElement.jsonObjectOrNull: JsonObject? get() = this as? JsonObject
private val JsonElement.jsonArrayOrNull: JsonArray? get() = this as? JsonArray
private val JsonElement.strOrNull: String? get() = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
private val JsonElement.intOrNull: Int? get() = (this as? JsonPrimitive)?.content?.toIntOrNull()
