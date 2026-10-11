package com.example.ui.components

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.theme.CityColors

/** A local, decorative video. The poster is always present behind the decoding surface. */
@Composable
fun WelcomeBackdrop(modifier: Modifier = Modifier, playbackAllowed: Boolean = true) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var failed by remember { mutableStateOf(false) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val play = LocalCityMotion.current && !LocalInspectionMode.current && resumed && playbackAllowed && !failed
    val canvas = CityColors.canvas
    Box(modifier) {
        Image(painterResource(R.drawable.welcome_neighborhood_poster), contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().testTag("welcome_video_poster"))
        if (play) {
            AndroidView(
                factory = { context -> WelcomeVideoView(context) { failed = true } },
                modifier = Modifier.fillMaxSize().testTag("welcome_video_surface"),
                update = { it.bindLifecycle(lifecycle) },
                onRelease = { it.release() }
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to canvas.copy(alpha = .84f),
            .38f to canvas.copy(alpha = .76f),
            .64f to canvas.copy(alpha = .35f),
            .80f to canvas.copy(alpha = .62f),
            1f to canvas.copy(alpha = .84f)
        )))
    }
}

/** TextureView allows the Compose scrim to cover the video and avoids a separate surface window. */
internal class WelcomeVideoView(context: Context, private val onFailure: () -> Unit) :
    TextureView(context), TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var output: Surface? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private var lifecycle: Lifecycle? = null
    private val lifecycleObserver = LifecycleEventObserver { _, _ ->
        // Do not wait for a Compose frame: a stopped activity may no longer recompose.
        if (lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) releasePlayer()
        else if (player == null && isAvailable && isAttachedToWindow) {
            surfaceTexture?.let { onSurfaceTextureAvailable(it, width, height) }
        }
    }

    init {
        isOpaque = false
        alpha = 0f
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        isFocusable = false
        isClickable = false
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        releasePlayer()
        if (lifecycle != null && lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return
        try {
            val active = MediaPlayer()
            player = active
            output = Surface(texture)
            active.setSurface(output)
            context.resources.openRawResourceFd(R.raw.welcome_neighborhood).use { asset ->
                active.setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
            }
            active.setOnVideoSizeChangedListener { source, w, h ->
                if (source === player) { videoWidth = w; videoHeight = h; updateCrop() }
            }
            active.setOnPreparedListener { source ->
                if (source === player) {
                    try {
                        source.setVolume(0f, 0f)
                        source.isLooping = true
                        videoWidth = source.videoWidth
                        videoHeight = source.videoHeight
                        updateCrop()
                        source.start()
                    } catch (_: IllegalStateException) { fail() }
                }
            }
            active.setOnInfoListener { source, what, _ ->
                if (source === player && what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) alpha = 1f
                false
            }
            active.setOnErrorListener { source, _, _ ->
                if (source === player) fail()
                true
            }
            active.prepareAsync()
        } catch (_: Exception) { fail() }
    }

    private fun updateCrop() {
        if (width <= 0 || height <= 0 || videoWidth <= 0 || videoHeight <= 0) return
        val scale = maxOf(width.toFloat() / videoWidth, height.toFloat() / videoHeight)
        setTransform(Matrix().apply {
            setScale(videoWidth * scale / width, videoHeight * scale / height, width / 2f, height / 2f)
        })
    }

    private fun fail() { releasePlayer(); onFailure() }

    private fun releasePlayer() {
        alpha = 0f
        val old = player
        player = null
        old?.setOnPreparedListener(null)
        old?.setOnInfoListener(null)
        old?.setOnErrorListener(null)
        old?.setOnVideoSizeChangedListener(null)
        old?.release()
        output?.release()
        output = null
    }

    fun bindLifecycle(owner: Lifecycle) {
        if (lifecycle === owner) return
        lifecycle?.removeObserver(lifecycleObserver)
        lifecycle = owner
        owner.addObserver(lifecycleObserver)
    }

    fun release() {
        lifecycle?.removeObserver(lifecycleObserver)
        lifecycle = null
        surfaceTextureListener = null
        releasePlayer()
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = updateCrop()
    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean { releasePlayer(); return true }
    override fun onDetachedFromWindow() { releasePlayer(); super.onDetachedFromWindow() }
}
