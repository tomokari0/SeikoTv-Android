package com.example.presentation.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.domain.model.Content
import com.example.domain.model.Episode
import com.example.ui.theme.SeikoBlack
import com.example.ui.theme.SeikoDarkSurface
import com.example.ui.theme.SeikoRed
import com.example.ui.theme.TextDarkGray
import com.example.ui.theme.TextGray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    content: Content,
    initialEpisode: Episode? = null,
    onBack: () -> Unit,
    onSaveProgress: (String?, Long, Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = remember(content.id) {
        PlayerViewModel(com.example.SeikoApplication.instance.contentRepository)
    }
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(content.id, initialEpisode?.id) {
        viewModel.initPlayback(content, initialEpisode)
    }

    val uiState by viewModel.uiState.collectAsState()
    val activeContent = uiState.content ?: content
    val currentEpisode = uiState.currentEpisode
        ?: initialEpisode
        ?: activeContent.seasons.firstOrNull()?.episodes?.firstOrNull()
        ?: activeContent.seasons.flatMap { it.episodes }.firstOrNull()

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(content.progressMs) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var is2xSpeedActive by remember { mutableStateOf(false) }

    // Dialog & Drawer states
    var showEpisodeDrawer by remember { mutableStateOf(false) }
    var showSubtitlesDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    var selectedSubtitle by remember { mutableStateOf("none") }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var exoAudioTracks by remember { mutableStateOf<List<ExoAudioOption>>(emptyList()) }

    val availableAudioOptions = remember(activeContent, currentEpisode, exoAudioTracks) {
        AudioTrackManager.resolveAudioOptions(
            content = activeContent,
            episode = currentEpisode,
            exoTracks = exoAudioTracks
        )
    }

    val defaultAudioOption = remember(availableAudioOptions) {
        availableAudioOptions.firstOrNull { it.isOriginal || it.isDefault }
            ?: availableAudioOptions.firstOrNull()
    }
    var selectedAudio by remember(activeContent.id, currentEpisode?.id) {
        mutableStateOf(defaultAudioOption?.id ?: "orig")
    }

    // Audio & Brightness Controls
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var currentVolumeFraction by remember {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume
        )
    }
    var currentBrightness by remember {
        val window = activity?.window
        val lp = window?.attributes
        mutableFloatStateOf(if (lp != null && lp.screenBrightness >= 0) lp.screenBrightness else 0.5f)
    }

    // Configure Orientation & System Bars
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Picture-in-Picture mode state
    var isInPipMode by remember { mutableStateOf(activity?.isInPictureInPictureMode ?: false) }

    // Register Picture-in-Picture lifecycle and automatic enter on app minimization
    DisposableEffect(activity, isPlaying) {
        val componentActivity = activity as? ComponentActivity

        val pipListener = Consumer<PictureInPictureModeChangedInfo> { info ->
            isInPipMode = info.isInPictureInPictureMode
            if (info.isInPictureInPictureMode) {
                showControls = false
                showEpisodeDrawer = false
                showSubtitlesDialog = false
                showAudioDialog = false
                showSpeedDialog = false
            }
        }
        componentActivity?.addOnPictureInPictureModeChangedListener(pipListener)

        val userLeaveListener = Runnable {
            if (isPlaying && !isInPipMode) {
                showControls = false
                enterPipMode(activity)
            }
        }
        componentActivity?.addOnUserLeaveHintListener(userLeaveListener)

        // Android 12+ (API 31+) automatic PiP upon swipe-to-home gesture
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity != null) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .setAutoEnterEnabled(isPlaying)
                    .setSeamlessResizeEnabled(true)
                    .build()
                activity.setPictureInPictureParams(params)
            } catch (_: Exception) {}
        }

        onDispose {
            componentActivity?.removeOnPictureInPictureModeChangedListener(pipListener)
            componentActivity?.removeOnUserLeaveHintListener(userLeaveListener)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity != null) {
                try {
                    val params = PictureInPictureParams.Builder()
                        .setAutoEnterEnabled(false)
                        .build()
                    activity.setPictureInPictureParams(params)
                } catch (_: Exception) {}
            }
        }
    }

    // Determine current video URL using robust ViewModel resolver (guaranteed to never play intro clip as content)
    val activeVideoUrl = uiState.activeVideoUrl.ifEmpty {
        PlayerViewModel.resolvePlayableUrlStatic(activeContent, currentEpisode).url
    }

    var nativeCueText by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember(context) {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
            com.example.data.datasource.download.DownloadManager.getInstance(context).createDataSourceFactory()
        )

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    viewModel.setPlaying(playing)
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        totalDurationMs = duration.coerceAtLeast(1L)
                    }
                }
                override fun onCues(cueGroup: CueGroup) {
                    nativeCueText = cueGroup.cues.firstOrNull()?.text?.toString()
                }
                override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                    val list = mutableListOf<ExoAudioOption>()
                    for (group in tracks.groups) {
                        if (group.type == C.TRACK_TYPE_AUDIO) {
                            for (i in 0 until group.length) {
                                val format = group.getTrackFormat(i)
                                val lang = format.language ?: ""
                                val label = format.label ?: ""
                                val id = "exo_${format.id ?: "${lang}_$i"}"
                                list.add(
                                    ExoAudioOption(
                                        id = id,
                                        language = lang,
                                        label = label,
                                        trackGroup = group.mediaTrackGroup,
                                        trackIndex = i
                                    )
                                )
                            }
                        }
                    }
                    exoAudioTracks = list
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    viewModel.handlePlayerError(error)
                    try {
                        val rawUri = androidx.media3.datasource.RawResourceDataSource.buildRawResourceUri(com.example.R.raw.sample_offline)
                        setMediaItem(MediaItem.fromUri(rawUri))
                        prepare()
                        play()
                    } catch (e: Exception) {
                        val fallback = PlayerViewModel.getFallbackStream(activeContent.id, currentEpisode?.id)
                        setMediaItem(MediaItem.fromUri(fallback))
                        prepare()
                        play()
                    }
                }
            })
        }
    }

    LaunchedEffect(activeVideoUrl, currentEpisode?.id) {
        if (activeVideoUrl.isNotBlank()) {
            val currentMediaItem = exoPlayer.currentMediaItem
            val currentUri = currentMediaItem?.localConfiguration?.uri?.toString()
            val currentMediaId = currentMediaItem?.mediaId
            val targetMediaId = currentEpisode?.id ?: activeVideoUrl

            if (currentUri != activeVideoUrl || currentMediaId != targetMediaId) {
                val mediaItem = MediaItem.Builder()
                    .setUri(activeVideoUrl)
                    .setMediaId(targetMediaId)
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                val targetSeek = if ((currentEpisode?.progressMs ?: 0L) > 0L) {
                    currentEpisode!!.progressMs
                } else if (currentEpisode == null && content.progressMs > 0L) {
                    content.progressMs
                } else {
                    0L
                }
                exoPlayer.seekTo(targetSeek)
                exoPlayer.play()
            }
        }
    }

    // Dynamic Subtitle track selection in ExoPlayer & ViewModel
    LaunchedEffect(selectedSubtitle) {
        viewModel.selectSubtitle(selectedSubtitle)
        val isDisabled = selectedSubtitle == "none"
        val preferredLang = when (selectedSubtitle) {
            "es_auto", "es_orig" -> "es"
            "en_trans" -> "en"
            "ja_trans" -> "ja"
            else -> "es"
        }
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, isDisabled)
            .setPreferredTextLanguage(if (isDisabled) null else preferredLang)
            .build()
    }

    // Dynamic Audio track selection in ExoPlayer & ViewModel with feedback toast
    LaunchedEffect(selectedAudio, availableAudioOptions) {
        val selectedOpt = availableAudioOptions.firstOrNull { it.id == selectedAudio }
            ?: availableAudioOptions.firstOrNull { it.language.equals(selectedAudio, ignoreCase = true) }
            ?: availableAudioOptions.firstOrNull()

        if (selectedOpt != null) {
            viewModel.selectAudioTrack(selectedOpt.id, selectedOpt.displayLabel)

            val matchingExo = exoAudioTracks.firstOrNull {
                it.id == selectedOpt.id ||
                (it.language.isNotBlank() && it.language.equals(selectedOpt.language, ignoreCase = true))
            }

            if (matchingExo != null) {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setPreferredAudioLanguage(matchingExo.language.ifEmpty { selectedOpt.language })
                    .setOverrideForType(
                        androidx.media3.common.TrackSelectionOverride(
                            matchingExo.trackGroup,
                            listOf(matchingExo.trackIndex)
                        )
                    )
                    .build()
            } else {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setPreferredAudioLanguage(selectedOpt.language)
                    .build()
            }
        }
    }

    // Ticking position loop & auto hide controls
    LaunchedEffect(exoPlayer, nativeCueText) {
        while (true) {
            currentPositionMs = exoPlayer.currentPosition
            if (exoPlayer.duration > 0) {
                totalDurationMs = exoPlayer.duration
            }
            viewModel.updatePosition(currentPositionMs, totalDurationMs, nativeCueText)
            delay(400)
        }
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            if (!showEpisodeDrawer && !showSubtitlesDialog && !showAudioDialog && !showSpeedDialog) {
                showControls = false
            }
        }
    }

    fun handleBack() {
        onSaveProgress(currentEpisode?.id, exoPlayer.currentPosition, totalDurationMs)
        exoPlayer.stop()
        onBack()
    }

    BackHandler {
        if (showEpisodeDrawer) {
            showEpisodeDrawer = false
        } else {
            handleBack()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            onSaveProgress(currentEpisode?.id, exoPlayer.currentPosition, totalDurationMs)
            exoPlayer.release()
        }
    }

    // Skip Intro segment check
    val skipSegments = currentEpisode?.skipSegments ?: content.skipSegments
    val isIntroActive = skipSegments != null &&
            currentPositionMs >= (skipSegments.introStart * 1000).toLong() &&
            currentPositionMs <= (skipSegments.introEnd * 1000).toLong()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SeikoBlack)
    ) {
        // Media3 PlayerView (TextureView surface to prevent Codec2 HAL buffer errors on emulator)
        AndroidView(
            factory = { ctx ->
                try {
                    val view = android.view.LayoutInflater.from(ctx)
                        .inflate(com.example.R.layout.view_player_texture, null, false) as PlayerView
                    view.player = exoPlayer
                    view.layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    view
                } catch (e: Exception) {
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isInPipMode) {
            // Gesture Overlay
            PlayerGesturesOverlay(
            onSingleTap = { showControls = !showControls },
            onDoubleTapLeft = {
                val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                exoPlayer.seekTo(newPos)
            },
            onDoubleTapRight = {
                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(totalDurationMs)
                exoPlayer.seekTo(newPos)
            },
            onLongPressStart = {
                is2xSpeedActive = true
                exoPlayer.setPlaybackSpeed(2.0f)
            },
            onLongPressEnd = {
                is2xSpeedActive = false
                exoPlayer.setPlaybackSpeed(currentSpeed)
            },
            onBrightnessChange = { delta ->
                val newB = (currentBrightness + delta).coerceIn(0.05f, 1.0f)
                currentBrightness = newB
                activity?.window?.attributes = activity?.window?.attributes?.apply {
                    screenBrightness = newB
                }
            },
            onVolumeChange = { delta ->
                val newVolFraction = (currentVolumeFraction + delta).coerceIn(0f, 1f)
                currentVolumeFraction = newVolFraction
                val targetVol = (newVolFraction * maxVolume).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            },
            currentBrightness = currentBrightness,
            currentVolume = currentVolumeFraction,
            is2xSpeedActive = is2xSpeedActive
        )

        // Cinematic Subtitle Overlay (CC / Traducciones sincronizadas)
        val activeSubtitle = uiState.currentSubtitleText
        if (!activeSubtitle.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = if (showControls) 96.dp else 40.dp,
                        start = 24.dp,
                        end = 24.dp
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.78f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = activeSubtitle,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 23.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black,
                                blurRadius = 6f
                            )
                        )
                    )
                }
            }
        }

        // Animated Audio Notification Toast
        AnimatedVisibility(
            visible = uiState.audioFeedbackToast != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            uiState.audioFeedbackToast?.let { toast ->
                LaunchedEffect(toast) {
                    delay(3000)
                    viewModel.clearAudioToast()
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SeikoDarkSurface.copy(alpha = 0.95f))
                        .border(1.dp, SeikoRed, RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = SeikoRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = toast,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Floating "Saltar intro" button
        AnimatedVisibility(
            visible = isIntroActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .border(1.dp, SeikoRed, RoundedCornerShape(8.dp))
                    .clickable {
                        skipSegments?.let {
                            exoPlayer.seekTo((it.introEnd * 1000).toLong())
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = null,
                        tint = SeikoRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Saltar intro",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Overlay Cinematic Player Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Top Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close button
                    IconButton(onClick = { handleBack() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar reproductor",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Title
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = content.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        currentEpisode?.let { ep ->
                            Text(
                                text = "T1 • E${ep.episodeNumber}: ${ep.title}",
                                color = SeikoRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // PiP action button
                    IconButton(onClick = {
                        showControls = false
                        enterPipMode(activity)
                    }) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Picture in Picture",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Episode drawer button
                    IconButton(onClick = { showEpisodeDrawer = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuOpen,
                            contentDescription = "Episodios y Detalles",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Center Play/Pause & Seek buttons
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    // Seek -10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Retroceder 10 segundos",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Play/Pause Big Button
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(SeikoRed)
                            .clickable {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Seek +10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(totalDurationMs)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Adelantar 10 segundos",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Bottom Timeline and Controls (matching Screenshots 7-10)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Glowing Red Slider
                    val sliderProgress = if (totalDurationMs > 0) {
                        (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = sliderProgress,
                        onValueChange = { fraction ->
                            val target = (fraction * totalDurationMs).toLong()
                            currentPositionMs = target
                            exoPlayer.seekTo(target)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = SeikoRed,
                            inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Bottom Row: Times + Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Play/Pause mini + Time elapsed
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatDuration(currentPositionMs),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = " / ${formatDuration(totalDurationMs)}",
                                color = TextDarkGray,
                                fontSize = 12.sp
                            )
                        }

                        // Right actions: Audio, CC, Speed, Fullscreen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Audio Tracks button
                            IconButton(
                                onClick = { showAudioDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = "Pistas de Audio",
                                    tint = if (selectedAudio != "ja") SeikoRed else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Subtitles (CC) button
                            IconButton(
                                onClick = { showSubtitlesDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Subtítulos",
                                    tint = if (selectedSubtitle != "none") SeikoRed else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Speed Gear button
                            IconButton(
                                onClick = { showSpeedDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Ajustes",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Side Drawer for Details & Episodes (Screenshots 8, 9, 10)
        AnimatedVisibility(
            visible = showEpisodeDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            EpisodeDrawer(
                content = activeContent,
                currentEpisode = currentEpisode,
                onSelectEpisode = { newEpisode ->
                    showEpisodeDrawer = false
                    viewModel.selectEpisode(newEpisode)
                },
                onClose = { showEpisodeDrawer = false }
            )
        }

        // Subtitles Dialog
        if (showSubtitlesDialog) {
            SubtitlesDialog(
                selectedSubtitle = selectedSubtitle,
                onSelectSubtitle = { selectedSubtitle = it },
                onDismiss = { showSubtitlesDialog = false }
            )
        }

        // Audio Tracks Dialog
        if (showAudioDialog) {
            AudioTrackDialog(
                selectedAudio = selectedAudio,
                availableTracks = availableAudioOptions,
                onSelectAudio = { trackOption ->
                    selectedAudio = trackOption.id
                },
                onDismiss = { showAudioDialog = false }
            )
        }

        // Playback Speed Dialog
        if (showSpeedDialog) {
            PlaybackSpeedDialog(
                currentSpeed = currentSpeed,
                onSelectSpeed = { newSpeed ->
                    currentSpeed = newSpeed
                    exoPlayer.setPlaybackSpeed(newSpeed)
                },
                onDismiss = { showSpeedDialog = false }
            )
        }
        }
    }
}

/**
 * Triggers Android Picture-in-Picture mode on supported API levels
 * with a 16:9 aspect ratio and seamless resize enabled.
 */
fun enterPipMode(activity: Activity?): Boolean {
    if (activity == null) return false
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setAutoEnterEnabled(true)
                        setSeamlessResizeEnabled(true)
                    }
                }
                .build()
            activity.enterPictureInPictureMode(params)
        } else {
            @Suppress("DEPRECATION")
            activity.enterPictureInPictureMode()
            true
        }
    } catch (_: Exception) {
        try {
            @Suppress("DEPRECATION")
            activity.enterPictureInPictureMode()
            true
        } catch (_: Exception) {
            false
        }
    }
}

private fun isIntroUrl(url: String?): Boolean = PlayerViewModel.isAppIntroVideo(url)

private fun isUnsupportedEmbed(url: String): Boolean = PlayerViewModel.isUnsupportedEmbed(url)

private fun ensureAbsoluteUrl(rawUrl: String): String = PlayerViewModel.ensureAbsoluteCdnUrl(rawUrl)

private fun resolvePlayableUrl(content: Content, episode: Episode?): String {
    return PlayerViewModel.resolvePlayableUrlStatic(content, episode).url
}

private fun getFallbackStream(contentId: String, episodeId: String?): String {
    return PlayerViewModel.getFallbackStream(contentId, episodeId)
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
