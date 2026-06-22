package com.ho.lolive

import android.content.Context
import android.content.res.ColorStateList
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.DrmConfiguration
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.android.material.button.MaterialButton
import com.ho.lolive.domain.model.LiveRoomDetail
import com.ho.lolive.presentation.detail.DetailUiState
import com.ho.lolive.presentation.detail.DetailViewModel
import com.ho.lolive.presentation.detail.FullScreenMode
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetailActivity : ComponentActivity() {
    private enum class GestureMode {
        HORIZONTAL_SWITCH,
        VERTICAL_ADJUST,
    }

    private enum class VerticalAdjustSide {
        LEFT_BRIGHTNESS,
        RIGHT_VOLUME,
    }

    private val viewModel: DetailViewModel by viewModels()

    private lateinit var rootView: View
    private lateinit var playerView: PlayerView
    private lateinit var playerTopBar: View
    private lateinit var controlDock: View
    private lateinit var loadingView: View
    private lateinit var playerErrorText: TextView
    private lateinit var gestureHintText: TextView
    private lateinit var backButton: ImageButton
    private lateinit var playerTitleText: TextView
    private lateinit var previousButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var pauseButton: MaterialButton
    private lateinit var retryButton: MaterialButton
    private lateinit var fullscreenButton: MaterialButton

    private lateinit var player: ExoPlayer
    private lateinit var playerListener: Player.Listener
    private lateinit var audioManager: AudioManager

    private var latestUiState: DetailUiState = DetailUiState()
    private var isPlayerBuffering = false
    private var lastAppliedRoomId: String? = null
    private var lastAppliedRetryToken = -1
    private var maxStreamVolume = 1
    private var touchSlop = 0
    private var horizontalSwitchThresholdPx = 0f
    private var activeGestureMode: GestureMode? = null
    private var verticalAdjustSide: VerticalAdjustSide = VerticalAdjustSide.RIGHT_VOLUME
    private var gestureStartX = 0f
    private var gestureStartY = 0f
    private var gestureStartVolume = 0
    private var lastAppliedStreamVolume = 0
    private var currentBrightness = 0.5f
    private var gestureStartBrightness = 0.5f
    private var switchedInCurrentGesture = false
    private var movedBeyondSlopInCurrentGesture = false
    private var shouldResumeOnStart = false
    private var controlsVisible = true
    private val controlsFadeDurationMs = 180L
    private val hideGestureHintRunnable = Runnable { gestureHintText.isVisible = false }
    private val hideControlsRunnable = Runnable { setControlsVisible(false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detail)

        bindViews()
        setupInsets()
        setupPlayer()
        setupGestureControls()
        setupActions()
        observeState()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun bindViews() {
        rootView = findViewById(R.id.detailRoot)
        playerView = findViewById(R.id.playerView)
        playerTopBar = findViewById(R.id.playerTopBar)
        controlDock = findViewById(R.id.controlDock)
        loadingView = findViewById(R.id.detailLoadingView)
        playerErrorText = findViewById(R.id.playerErrorText)
        gestureHintText = findViewById(R.id.gestureHintText)
        backButton = findViewById(R.id.playerBackButton)
        playerTitleText = findViewById(R.id.playerTitleText)
        previousButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
        pauseButton = findViewById(R.id.pauseButton)
        retryButton = findViewById(R.id.retryButton)
        fullscreenButton = findViewById(R.id.fullscreenButton)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            rootView.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )
            insets
        }
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
        playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlayerBuffering = playbackState == Player.STATE_BUFFERING
                renderLoadingState()
            }

            override fun onPlayerError(error: PlaybackException) {
                viewModel.onPlayerError(error.message ?: error.errorCodeName)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePauseButton(isPlaying || player.playWhenReady)
            }
        }
        player.addListener(playerListener)
        playerView.player = player
        updatePauseButton(true)
    }

    private fun setupGestureControls() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxStreamVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        horizontalSwitchThresholdPx = resources.displayMetrics.density * 64f
        val initialBrightness = window.attributes.screenBrightness
        currentBrightness = if (initialBrightness in 0f..1f) initialBrightness else 0.5f

        playerView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    activeGestureMode = null
                    switchedInCurrentGesture = false
                    movedBeyondSlopInCurrentGesture = false
                    gestureStartX = event.x
                    gestureStartY = event.y
                    gestureStartVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    lastAppliedStreamVolume = gestureStartVolume
                    verticalAdjustSide = if (event.x < playerView.width / 2f) {
                        VerticalAdjustSide.LEFT_BRIGHTNESS
                    } else {
                        VerticalAdjustSide.RIGHT_VOLUME
                    }
                    gestureStartBrightness = currentBrightness
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.x - gestureStartX
                    val deltaY = event.y - gestureStartY

                    if (abs(deltaX) >= touchSlop || abs(deltaY) >= touchSlop) {
                        movedBeyondSlopInCurrentGesture = true
                    }

                    if (activeGestureMode == null) {
                        if (abs(deltaX) < touchSlop && abs(deltaY) < touchSlop) {
                            return@setOnTouchListener false
                        }
                        activeGestureMode = if (abs(deltaX) > abs(deltaY)) {
                            GestureMode.HORIZONTAL_SWITCH
                        } else {
                            GestureMode.VERTICAL_ADJUST
                        }
                    }

                    when (activeGestureMode) {
                        GestureMode.HORIZONTAL_SWITCH -> {
                            if (!switchedInCurrentGesture) {
                                when {
                                    deltaX <= -horizontalSwitchThresholdPx -> {
                                        viewModel.playNext()
                                        showGestureHint(getString(R.string.next_room))
                                        switchedInCurrentGesture = true
                                    }

                                    deltaX >= horizontalSwitchThresholdPx -> {
                                        viewModel.playPrevious()
                                        showGestureHint(getString(R.string.prev_room))
                                        switchedInCurrentGesture = true
                                    }
                                }
                            }
                        }

                        GestureMode.VERTICAL_ADJUST -> {
                            val ratio = (gestureStartY - event.y) / playerView.height.coerceAtLeast(1)
                            when (verticalAdjustSide) {
                                VerticalAdjustSide.LEFT_BRIGHTNESS -> {
                                    val targetBrightness = (gestureStartBrightness + ratio).coerceIn(0.05f, 1f)
                                    if (targetBrightness != currentBrightness) {
                                        currentBrightness = targetBrightness
                                        window.attributes = window.attributes.apply {
                                            screenBrightness = currentBrightness
                                        }
                                    }
                                    val brightnessPercent = (currentBrightness * 100f).roundToInt()
                                    showGestureHint(getString(R.string.brightness_percent, brightnessPercent))
                                }

                                VerticalAdjustSide.RIGHT_VOLUME -> {
                                    val targetVolume = (gestureStartVolume + ratio * maxStreamVolume)
                                        .roundToInt()
                                        .coerceIn(0, maxStreamVolume)
                                    // Avoid a per-move IPC to read the current volume; track it locally.
                                    if (targetVolume != lastAppliedStreamVolume) {
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                                        lastAppliedStreamVolume = targetVolume
                                    }
                                    val volumePercent = (targetVolume * 100f / maxStreamVolume).roundToInt()
                                    showGestureHint(getString(R.string.volume_percent, volumePercent))
                                }
                            }
                        }

                        null -> Unit
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (event.actionMasked == MotionEvent.ACTION_UP) {
                        if (!movedBeyondSlopInCurrentGesture && !switchedInCurrentGesture) {
                            toggleControls()
                        } else {
                            showControlsTemporarily()
                        }
                    }
                    activeGestureMode = null
                    switchedInCurrentGesture = false
                    movedBeyondSlopInCurrentGesture = false
                }
            }
            true
        }
    }

    private fun showGestureHint(message: String) {
        gestureHintText.text = message
        gestureHintText.isVisible = true
        gestureHintText.removeCallbacks(hideGestureHintRunnable)
        gestureHintText.postDelayed(hideGestureHintRunnable, 900)
    }

    private fun setupActions() {
        backButton.setOnClickListener { finish() }
        previousButton.setOnClickListener {
            viewModel.playPrevious()
            showControlsTemporarily()
        }
        nextButton.setOnClickListener {
            viewModel.playNext()
            showControlsTemporarily()
        }
        pauseButton.setOnClickListener {
            togglePlayback()
            showControlsTemporarily()
        }
        retryButton.setOnClickListener {
            viewModel.manualRetry()
            showControlsTemporarily()
        }
        fullscreenButton.setOnClickListener {
            if (latestUiState.fullScreenMode == FullScreenMode.NONE) {
                viewModel.setPortraitFullscreen()
            } else {
                viewModel.exitFullscreen()
            }
            showControlsTemporarily()
        }
        showControlsTemporarily()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    latestUiState = state
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: DetailUiState) {
        applyFullscreen(state.fullScreenMode)
        val enterFullscreen = state.fullScreenMode == FullScreenMode.NONE
        fullscreenButton.text = if (enterFullscreen) {
            getString(R.string.fullscreen)
        } else {
            getString(R.string.exit_fullscreen)
        }
        fullscreenButton.contentDescription = fullscreenButton.text

        val room = state.room
        if (room == null) {
            if (state.loading) {
                playerTitleText.text = getString(R.string.loading_rooms)
                playerErrorText.isVisible = false
            } else {
                playerTitleText.text = getString(R.string.play_failed)
                playerErrorText.isVisible = true
                playerErrorText.text = state.errorMessage ?: getString(R.string.play_failed)
            }
            pauseButton.isEnabled = false
            updatePauseButton(false)
        } else {
            playerTitleText.text = room.title
            playerErrorText.isVisible = false
            maybeApplyMedia(room, state.retryToken)
            pauseButton.isEnabled = true
        }

        previousButton.isEnabled = state.previousRoomId != null
        nextButton.isEnabled = state.nextRoomId != null

        if (state.playerErrorMessage != null) {
            val displayMessage = if (state.maxRetryReached) {
                getString(R.string.max_retry_reached, state.playerErrorMessage)
            } else {
                state.playerErrorMessage
            }
            Toast.makeText(this, displayMessage, Toast.LENGTH_SHORT).show()
            viewModel.dismissPlayerError()
            playerErrorText.isVisible = true
            playerErrorText.text = displayMessage
        }

        renderLoadingState()
    }

    private fun renderLoadingState() {
        loadingView.isVisible = latestUiState.loading || isPlayerBuffering
    }

    private fun maybeApplyMedia(room: LiveRoomDetail, retryToken: Int) {
        val shouldReload = room.id != lastAppliedRoomId || retryToken != lastAppliedRetryToken
        if (!shouldReload) return

        lastAppliedRoomId = room.id
        lastAppliedRetryToken = retryToken

        val mediaItemBuilder = MediaItem.Builder().setUri(room.streamUrl)
        room.drmLicenseUrl?.let { drmLicense ->
            mediaItemBuilder.setDrmConfiguration(
                DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(drmLicense)
                    .build(),
            )
        }

        player.setMediaItem(mediaItemBuilder.build())
        player.prepare()
        player.playWhenReady = true
        updatePauseButton(true)
    }

    private fun togglePlayback() {
        if (player.isPlaying || player.playWhenReady) {
            player.pause()
            player.playWhenReady = false
            updatePauseButton(false)
            return
        }
        player.playWhenReady = true
        player.play()
        updatePauseButton(true)
    }

    private fun updatePauseButton(playing: Boolean) {
        pauseButton.text = if (playing) getString(R.string.pause) else getString(R.string.play)
        pauseButton.contentDescription = pauseButton.text
    }

    private fun applyFullscreen(mode: FullScreenMode) {
        requestedOrientation = when (mode) {
            FullScreenMode.NONE -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            FullScreenMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            FullScreenMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (mode == FullScreenMode.NONE) {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun toggleControls() {
        setControlsVisible(!controlsVisible)
        if (controlsVisible) {
            scheduleControlsAutoHide()
        } else {
            rootView.removeCallbacks(hideControlsRunnable)
        }
    }

    private fun showControlsTemporarily() {
        setControlsVisible(true)
        scheduleControlsAutoHide()
    }

    private fun scheduleControlsAutoHide() {
        rootView.removeCallbacks(hideControlsRunnable)
        rootView.postDelayed(hideControlsRunnable, 3500L)
    }

    private fun setControlsVisible(visible: Boolean) {
        controlsVisible = visible
        animateControlVisibility(playerTopBar, visible)
        animateControlVisibility(controlDock, visible)
    }

    private fun animateControlVisibility(view: View, visible: Boolean) {
        view.animate().cancel()
        if (visible) {
            if (!view.isVisible) {
                view.alpha = 0f
                view.isVisible = true
            }
            view.animate()
                .alpha(1f)
                .setDuration(controlsFadeDurationMs)
                .start()
            return
        }

        if (!view.isVisible) return
        view.animate()
            .alpha(0f)
            .setDuration(controlsFadeDurationMs)
            .withEndAction {
                if (!controlsVisible) {
                    view.isVisible = false
                }
            }
            .start()
    }

    override fun onStart() {
        super.onStart()
        if (!::player.isInitialized || !shouldResumeOnStart) return
        player.playWhenReady = true
        player.play()
        updatePauseButton(true)
    }

    override fun onStop() {
        if (::player.isInitialized) {
            shouldResumeOnStart = player.playWhenReady
            player.pause()
            player.playWhenReady = false
            updatePauseButton(false)
        }
        super.onStop()
    }

    override fun onDestroy() {
        gestureHintText.removeCallbacks(hideGestureHintRunnable)
        rootView.removeCallbacks(hideControlsRunnable)
        shouldResumeOnStart = false
        playerView.player = null
        player.removeListener(playerListener)
        player.release()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        WindowCompat.getInsetsController(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
        // Restore system brightness that may have been changed by the brightness gesture.
        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    companion object {
        private const val ROOM_ID_KEY = "roomId"

        fun createIntent(context: Context, roomId: String): Intent {
            return Intent(context, DetailActivity::class.java)
                .putExtra(ROOM_ID_KEY, Uri.encode(roomId))
        }
    }
}
