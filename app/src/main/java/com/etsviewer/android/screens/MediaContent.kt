package com.etsviewer.android.screens

import com.etsviewer.android.R
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.etsviewer.core.answers.*
import com.etsviewer.core.feedback.*
import com.etsviewer.android.ui.*
import kotlinx.coroutines.delay
import java.io.File

@Composable internal fun MediaContent(media: MediaReference, root: String, boundary: String) {
    val texts = rememberUiStrings()
    val source = remember(media, root, boundary) { MediaLocations.resolve(media.reference, root, boundary.ifBlank { root }) }
    if (source == null) { CompactMediaError(when (media.kind) { MediaKind.IMAGE -> texts.text(R.string.image_error); MediaKind.AUDIO -> texts.text(R.string.audio_error); MediaKind.VIDEO -> texts.text(R.string.video_error) }, AppFailure(FeedbackOperation.MEDIA), media.reference.hashCode().toString()); return }
    when (media.kind) {
        MediaKind.IMAGE -> {
            var failed by remember(source) { mutableStateOf(false) }
            if (failed) CompactMediaError(texts.text(R.string.image_error), AppFailure(FeedbackOperation.MEDIA), source.hashCode().toString(), { failed = false })
            else AsyncImage(model = if (source.startsWith("file:")) File(Uri.parse(source).path.orEmpty()) else source,
                contentDescription = texts.text(R.string.image_description), modifier = Modifier.fillMaxWidth().heightIn(min = AppTokens.imageMin, max = AppTokens.imageMax).clip(MaterialTheme.shapes.medium),
                onError = { failed = true })
        }
        MediaKind.AUDIO -> AudioControl(source)
        MediaKind.VIDEO -> VideoControl(source)
    }
}

@Composable private fun AudioControl(source: String) {
    val texts = rememberUiStrings()
    val context = LocalContext.current
    var player by remember(source) { mutableStateOf<MediaPlayer?>(null) }
    var ready by remember(source) { mutableStateOf(false) }
    var playing by remember(source) { mutableStateOf(false) }
    var error by remember(source) { mutableStateOf<AppFailure?>(null) }
    var generation by remember(source) { mutableIntStateOf(0) }
    DisposableEffect(source, generation) {
        ready = false; playing = false; error = null
        val instance = MediaPlayer(); player = instance
        try {
            instance.setDataSource(context, Uri.parse(source))
            instance.setOnPreparedListener { ready = true }
            instance.setOnCompletionListener { playing = false }
            instance.setOnErrorListener { _, _, _ -> error = AppFailure(FeedbackOperation.MEDIA); ready = false; playing = false; true }
            instance.prepareAsync()
        } catch (failure: Exception) { error = FailureClassifier.classify(failure, FeedbackOperation.MEDIA) }
        onDispose { instance.release(); player = null }
    }
    if (error != null) CompactMediaError(texts.text(R.string.audio_error), error!!, source.hashCode().toString(), { generation++ })
    else AppSecondaryButton(if (playing) texts.text(R.string.audio_pause) else if (ready) texts.text(R.string.audio_play) else texts.text(R.string.audio_loading), {
        runCatching { if (playing) player?.pause() else player?.start(); playing = !playing }.onFailure { error = FailureClassifier.classify(it, FeedbackOperation.MEDIA) }
    }, enabled = ready)
}

/** VideoView renders video only; all playback controls use the shared Compose theme. */
@Composable private fun VideoControl(source: String) {
    val texts = rememberUiStrings()
    var video by remember(source) { mutableStateOf<VideoView?>(null) }
    var ready by remember(source) { mutableStateOf(false) }
    var playing by remember(source) { mutableStateOf(false) }
    var failed by remember(source) { mutableStateOf(false) }
    var position by remember(source) { mutableFloatStateOf(0f) }
    var duration by remember(source) { mutableIntStateOf(0) }
    var generation by remember(source) { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.small)) {
        key(source, generation) {
            AndroidView(factory = { context -> VideoView(context).apply {
                video = this; ready = false; failed = false; playing = false
                setVideoURI(Uri.parse(source))
                setOnPreparedListener { duration = it.duration; ready = true }
                setOnCompletionListener { playing = false }
                setOnErrorListener { _, _, _ -> failed = true; ready = false; playing = false; true }
            } }, modifier = Modifier.fillMaxWidth().height(if (failed) AppTokens.flat else AppTokens.mediaHeight).clip(MaterialTheme.shapes.medium), onRelease = { it.stopPlayback(); video = null })
        }
        if (failed) CompactMediaError(texts.text(R.string.video_error), AppFailure(FeedbackOperation.MEDIA), source.hashCode().toString(), { generation++ })
        else {
            AppSecondaryButton(if (playing) texts.text(R.string.video_pause) else if (ready) texts.text(R.string.video_play) else texts.text(R.string.video_loading), {
                runCatching { if (playing) video?.pause() else video?.start(); playing = !playing }.onFailure { failed = true }
            }, enabled = ready)
            if (duration > 0) Slider(position, { position = it; video?.seekTo(it.toInt()) }, enabled = ready,
                valueRange = 0f..duration.toFloat(), modifier = Modifier.heightIn(min = AppTokens.touch).semantics { contentDescription = texts.text(R.string.video_progress) })
            if (duration > 0) Text("${mediaTime(position.toInt())} / ${mediaTime(duration)}", style = MaterialTheme.typography.labelMedium)
        }
    }
    LaunchedEffect(video, ready) {
        while (ready && video != null) { position = video?.currentPosition?.toFloat() ?: 0f; delay(500) }
    }
}
private fun mediaTime(milliseconds: Int): String {
    val seconds = (milliseconds / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
