package com.spoz.desktop

import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer

/**
 * Playback on the JavaFX Media engine.
 *
 * JavaFX Media is used because its native decoders (AAC/MP3) ship inside the
 * openjfx jars, so the packaged installer needs nothing installed on the user's
 * machine - unlike VLCJ (needs VLC) or gst1-java (needs GStreamer).
 */
class AudioPlayer {
    private var player: MediaPlayer? = null
    var onFinished: (() -> Unit)? = null

    fun play(url: String, onError: (String) -> Unit = {}) {
        stop()
        try {
            val media = Media(url)
            val mp = MediaPlayer(media)
            mp.setOnEndOfMedia { Platform.runLater { onFinished?.invoke() } }
            mp.setOnError { onError(mp.error?.message ?: "playback error") }
            mp.play()
            player = mp
        } catch (t: Throwable) {
            onError(t.message ?: t.toString())
        }
    }

    val isPlaying: Boolean get() = player?.status == MediaPlayer.Status.PLAYING

    fun toggle() {
        val mp = player ?: return
        if (mp.status == MediaPlayer.Status.PLAYING) mp.pause() else mp.play()
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; runCatching { it.dispose() } }
        player = null
    }
}
