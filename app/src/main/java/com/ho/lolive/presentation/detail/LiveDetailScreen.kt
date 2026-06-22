package com.ho.lolive.presentation.detail

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.ho.lolive.R
import com.ho.lolive.domain.model.LiveRoomDetail
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private data class ResolutionOption(
    val label: String,
    val group: TrackGroup,
    val trackIndex: Int,
)

private enum class GestureSide {
    LEFT,
    RIGHT,
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val fullScreenMode = uiState.fullScreenMode
    val isFullscreen = fullScreenMode != FullScreenMode.NONE

    DisposableEffect(fullScreenMode) {
        activity?.requestedOrientation = when (fullScreenMode) {
            FullScreenMode.NONE -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            FullScreenMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            FullScreenMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }

        activity?.window?.let { window ->
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                insetsController.hide(android.view.WindowInsets.Type.systemBars())
            } else {
                insetsController.show(android.view.WindowInsets.Type.systemBars())
            }
        }

        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(android.view.WindowInsets.Type.systemBars())
            }
        }
    }

    if (uiState.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val room = uiState.room
    if (room == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = uiState.errorMessage ?: stringResource(id = R.string.play_failed))
        }
        return
    }

    Scaffold(
        containerColor = Color(0xFFF3FAF7),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFEFFCF7), Color(0xFFDFF6F2), Color(0xFFF4FAF8)),
                    ),
                ),
        ) {
            RoomPlayer(
                room = room,
                retryToken = uiState.retryToken,
                fullScreenMode = fullScreenMode,
                canPlayPrevious = uiState.previousRoomId != null,
                canPlayNext = uiState.nextRoomId != null,
                onBack = onBack,
                onSetLandscapeFullscreen = viewModel::setLandscapeFullscreen,
                onSetPortraitFullscreen = viewModel::setPortraitFullscreen,
                onExitFullscreen = viewModel::exitFullscreen,
                onRetry = viewModel::manualRetry,
                onPlayPrevious = viewModel::playPrevious,
                onPlayNext = viewModel::playNext,
                onPlayerError = viewModel::onPlayerError,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isFullscreen) 1f else 0.72f),
            )

            if (!isFullscreen) {
                if (!uiState.networkConnected) {
                    Text(
                        text = stringResource(id = R.string.network_lost),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }

    uiState.playerErrorMessage?.let { msg ->
        val displayMessage = if (uiState.maxRetryReached) {
            stringResource(id = R.string.max_retry_reached, msg)
        } else {
            msg
        }
        AlertDialog(
            onDismissRequest = viewModel::dismissPlayerError,
            title = { Text(text = stringResource(id = R.string.play_failed)) },
            text = { Text(text = displayMessage) },
            confirmButton = {
                Button(onClick = viewModel::dismissPlayerError) {
                    Text(text = stringResource(id = R.string.close))
                }
            },
        )
    }
}

@Composable
private fun RoomPlayer(
    room: LiveRoomDetail,
    retryToken: Int,
    fullScreenMode: FullScreenMode,
    canPlayPrevious: Boolean,
    canPlayNext: Boolean,
    onBack: () -> Unit,
    onSetLandscapeFullscreen: () -> Unit,
    onSetPortraitFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
    onRetry: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayerError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxStreamVolume = remember {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    val initialPlayerVolume = remember {
        (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxStreamVolume)
            .coerceIn(0f, 1f)
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            volume = initialPlayerVolume
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var playerVolume by remember { mutableFloatStateOf(initialPlayerVolume) }
    var isMuted by remember { mutableStateOf(initialPlayerVolume <= 0.001f) }
    var dragDistanceX by remember { mutableFloatStateOf(0f) }
    var currentGestureSide by remember { mutableStateOf<GestureSide?>(null) }
    var gestureHint by remember { mutableStateOf<String?>(null) }
    var fullscreenMenuExpanded by remember { mutableStateOf(false) }
    var infoDialogVisible by remember { mutableStateOf(false) }
    var shouldResumeOnStart by remember { mutableStateOf(false) }

    var currentBrightness by remember {
        mutableFloatStateOf(
            ((activity?.window?.attributes?.screenBrightness ?: -1f).takeIf { it > 0f } ?: 0.5f),
        )
    }

    var resolutionOptions by remember { mutableStateOf<List<ResolutionOption>>(emptyList()) }
    var selectedResolutionText by remember { mutableStateOf("--") }
    var userSelectedResolution by remember(room.id) { mutableStateOf(false) }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3500)
            controlsVisible = false
        }
    }

    LaunchedEffect(gestureHint) {
        if (gestureHint != null) {
            delay(1000)
            gestureHint = null
        }
    }

    LaunchedEffect(retryToken, room.id) {
        val mediaItemBuilder = MediaItem.Builder().setUri(room.streamUrl)

        room.drmLicenseUrl?.let { drmLicense ->
            mediaItemBuilder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(drmLicense)
                    .build(),
            )
        }

        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(tracks: Tracks) {
                val collected = mutableListOf<ResolutionOption>()
                tracks.groups.forEach { group ->
                    if (group.type == C.TRACK_TYPE_VIDEO) {
                        for (index in 0 until group.length) {
                            val format = group.getTrackFormat(index)
                            if (format.width > 0 && format.height > 0) {
                                collected += ResolutionOption(
                                    label = "${format.height}p",
                                    group = group.mediaTrackGroup,
                                    trackIndex = index,
                                )
                            }
                        }
                    }
                }
                resolutionOptions = collected.distinctBy { it.label }
                    .sortedByDescending { option ->
                        option.label.substringBefore("p").toIntOrNull() ?: 0
                    }

                if (resolutionOptions.isNotEmpty() && !userSelectedResolution) {
                    val highest = resolutionOptions.first()
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                        .addOverride(
                            TrackSelectionOverride(
                                highest.group,
                                listOf(highest.trackIndex),
                            ),
                        )
                        .build()
                    selectedResolutionText = highest.label
                }

                val videoSize = exoPlayer.videoSize
                if (videoSize.width > 0 && videoSize.height > 0) {
                    selectedResolutionText = "${videoSize.height}p"
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                onPlayerError(error.message ?: error.errorCodeName)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    shouldResumeOnStart = exoPlayer.playWhenReady
                    exoPlayer.pause()
                    exoPlayer.playWhenReady = false
                }

                Lifecycle.Event.ON_START -> {
                    if (!shouldResumeOnStart) return@LifecycleEventObserver
                    exoPlayer.playWhenReady = true
                    exoPlayer.play()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(room.id) {
                detectTapGestures {
                    controlsVisible = !controlsVisible
                }
            }
            .pointerInput(room.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        dragDistanceX += dragAmount
                    },
                    onDragEnd = {
                        when {
                            dragDistanceX < -120f && canPlayPrevious -> onPlayPrevious()
                            dragDistanceX > 120f && canPlayNext -> onPlayNext()
                        }
                        dragDistanceX = 0f
                        controlsVisible = true
                    },
                    onDragCancel = { dragDistanceX = 0f },
                )
            }
            .pointerInput(room.id) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        currentGestureSide = if (offset.x < size.width / 2f) GestureSide.LEFT else GestureSide.RIGHT
                    },
                    onVerticalDrag = { change, dragAmount ->
                        val factor = (-dragAmount / size.height).coerceIn(-0.1f, 0.1f)
                        when (currentGestureSide) {
                            GestureSide.LEFT -> {
                                currentBrightness = (currentBrightness + factor).coerceIn(0.05f, 1f)
                                activity?.window?.let { window ->
                                    window.attributes = window.attributes.apply {
                                        screenBrightness = currentBrightness
                                    }
                                }
                                gestureHint = context.getString(
                                    R.string.brightness_percent,
                                    (currentBrightness * 100).roundToInt(),
                                )
                            }

                            GestureSide.RIGHT -> {
                                playerVolume = (playerVolume + factor * 2f).coerceIn(0f, 1f)
                                exoPlayer.volume = playerVolume
                                isMuted = playerVolume <= 0.001f
                                val streamVolume = (playerVolume * maxStreamVolume)
                                    .roundToInt()
                                    .coerceIn(0, maxStreamVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVolume, 0)
                                gestureHint = context.getString(
                                    R.string.volume_percent,
                                    (playerVolume * 100).roundToInt(),
                                )
                            }

                            null -> Unit
                        }
                        controlsVisible = true
                        change.consume()
                    },
                    onDragEnd = { currentGestureSide = null },
                    onDragCancel = { currentGestureSide = null },
                )
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.player = exoPlayer
            },
        )

        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(42.dp),
                color = Color.White,
            )
        }

        gestureHint?.let { hint ->
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Black.copy(alpha = 0.55f),
            ) {
                Text(
                    text = hint,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }

        if (controlsVisible) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.28f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                    Text(
                        text = room.title,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onPlayPrevious, enabled = canPlayPrevious) {
                        Icon(Icons.Default.NavigateBefore, contentDescription = null, tint = Color.White)
                    }
                    IconButton(onClick = onPlayNext, enabled = canPlayNext) {
                        Icon(Icons.Default.NavigateNext, contentDescription = null, tint = Color.White)
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.33f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            controlsVisible = true
                        }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                            )
                        }

                        IconButton(onClick = {
                            isMuted = !isMuted
                            playerVolume = if (isMuted) 0f else 1f
                            exoPlayer.volume = playerVolume
                            val streamVolume = (playerVolume * maxStreamVolume)
                                .roundToInt()
                                .coerceIn(0, maxStreamVolume)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVolume, 0)
                            controlsVisible = true
                        }) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color.White,
                            )
                        }

                        Box {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .height(34.dp)
                                    .widthIn(min = 64.dp)
                                    .clickable { fullscreenMenuExpanded = true },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = selectedResolutionText,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip,
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = fullscreenMenuExpanded,
                                onDismissRequest = { fullscreenMenuExpanded = false },
                            ) {
                                resolutionOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(text = option.label) },
                                        onClick = {
                                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                .buildUpon()
                                                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                                .addOverride(
                                                    TrackSelectionOverride(
                                                        option.group,
                                                        listOf(option.trackIndex),
                                                    ),
                                                )
                                                .build()
                                            userSelectedResolution = true
                                            selectedResolutionText = option.label
                                            fullscreenMenuExpanded = false
                                            controlsVisible = true
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { infoDialogVisible = true }) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                        }
                        IconButton(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        }

                        var fullMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = {
                                    if (fullScreenMode == FullScreenMode.NONE) {
                                        fullMenuExpanded = true
                                    } else {
                                        onExitFullscreen()
                                        fullMenuExpanded = false
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = if (fullScreenMode == FullScreenMode.NONE) {
                                        Icons.Default.Fullscreen
                                    } else {
                                        Icons.Default.FullscreenExit
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                )
                            }
                            DropdownMenu(
                                expanded = fullMenuExpanded,
                                onDismissRequest = { fullMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.fullscreen_landscape)) },
                                    onClick = {
                                        onSetLandscapeFullscreen()
                                        fullMenuExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(id = R.string.fullscreen_portrait)) },
                                    onClick = {
                                        onSetPortraitFullscreen()
                                        fullMenuExpanded = false
                                    },
                                )
                                if (fullScreenMode != FullScreenMode.NONE) {
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(id = R.string.exit_fullscreen)) },
                                        onClick = {
                                            onExitFullscreen()
                                            fullMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (infoDialogVisible) {
        val infoText = buildString {
            appendLine(stringResource(id = R.string.video_title, room.title))
            appendLine(stringResource(id = R.string.video_url, room.streamUrl))
            appendLine(stringResource(id = R.string.video_resolution, selectedResolutionText))
            append(stringResource(id = R.string.video_resolution_count, resolutionOptions.size))
        }

        AlertDialog(
            onDismissRequest = { infoDialogVisible = false },
            title = { Text(text = stringResource(id = R.string.video_info)) },
            text = { Text(text = infoText) },
            confirmButton = {
                Button(onClick = { infoDialogVisible = false }) {
                    Text(text = stringResource(id = R.string.close))
                }
            },
        )
    }
}
