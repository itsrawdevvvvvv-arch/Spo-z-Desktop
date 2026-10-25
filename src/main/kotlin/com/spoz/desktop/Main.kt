package com.spoz.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import javafx.application.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF0B0B0F)
private val Card = Color(0xFF18161F)
private val Purple = Color(0xFFB79CFF)
private val Muted = Color(0xFFA29CC0)

fun main() {
    // JavaFX owns audio; start its toolkit once, before the window opens.
    runCatching { Platform.startup { } }
    application {
        Window(onCloseRequest = ::exitApplication, title = "Spo~z") {
            MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Ink, surface = Card)) {
                App()
            }
        }
    }
}

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val player = remember { AudioPlayer() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Track>>(emptyList()) }
    var status by remember { mutableStateOf("Search YouTube Music to start") }
    var busy by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<Track?>(null) }

    fun play(track: Track) {
        scope.launch {
            status = "Resolving ${track.title}..."
            busy = true
            val url = withContext(Dispatchers.IO) { runCatching { YouTube.streamUrl(track.videoId) }.getOrNull() }
            busy = false
            if (url == null) {
                status = "Could not resolve a playable stream for \"${track.title}\""
            } else {
                current = track
                player.play(url) { status = "Finished: ${track.title}" }
                status = "Playing: ${track.title}"
            }
        }
    }

    player.onFinished = { results.getOrNull(0)?.let { } }

    Column(Modifier.fillMaxSize().background(Ink).padding(16.dp)) {
        Text("Spo~z", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Purple)
        Text("YouTube Music, on your desktop", color = Muted, fontSize = 13.sp)

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("Search songs, artists...") },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Button(
                enabled = query.isNotBlank() && !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        status = "Searching..."
                        results = withContext(Dispatchers.IO) {
                            runCatching { YouTube.search(query) }.getOrDefault(emptyList())
                        }
                        busy = false
                        status = if (results.isEmpty()) "No results" else "${results.size} results"
                    }
                },
            ) { Text("Search") }
        }

        Spacer(Modifier.height(14.dp))
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(results, key = { it.videoId }) { t ->
                Row(
                    Modifier.fillMaxWidth().clickable { play(t) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(t.title, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text(t.artist, color = Muted, fontSize = 12.sp)
                }
                HorizontalDivider(color = Color(0x22FFFFFF))
            }
        }

        Spacer(Modifier.height(10.dp))
        Surface(color = Card, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(current?.title ?: "Nothing playing", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(status, color = Muted, fontSize = 11.sp)
                }
                Button(onClick = { player.toggle() }) { Text("Play / Pause") }
            }
        }
    }
}
