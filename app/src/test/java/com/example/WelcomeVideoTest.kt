package com.example

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.CityModel
import com.example.ui.PreviewCityModel
import com.example.ui.components.LocalCityMotion
import com.example.ui.components.WelcomeBackdrop
import com.example.ui.components.WelcomeVideoView
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.CleanCityTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-port-xxhdpi")
class WelcomeVideoTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @After fun resetMedia() { ShadowMediaPlayer.resetStaticState() }

    private fun configurePlayer(onCreate: (MediaPlayer, ShadowMediaPlayer) -> Unit) {
        // Preparation is explicit in these tests; no decoder or video timing is emulated.
        ShadowMediaPlayer.setMediaInfoProvider { ShadowMediaPlayer.MediaInfo(8583, -1) }
        ShadowMediaPlayer.setCreateListener { player, shadow ->
            shadow.invalidStateBehavior = ShadowMediaPlayer.InvalidStateBehavior.ASSERT
            onCreate(player, shadow)
        }
    }

    @Test fun localVideoIsMutedLoopsAndReleasesItsPlayer() {
        lateinit var player: MediaPlayer
        lateinit var shadow: ShadowMediaPlayer
        configurePlayer { p, s -> player = p; shadow = s }
        compose.runOnUiThread {
            val texture = SurfaceTexture(0)
            val view = WelcomeVideoView(compose.activity) { fail("Valid local asset should prepare") }
            val owner = TestOwner().apply { registry.currentState = Lifecycle.State.RESUMED }
            view.bindLifecycle(owner.lifecycle)
            view.onSurfaceTextureAvailable(texture, 360, 800)
            assertEquals(ShadowMediaPlayer.State.PREPARING, shadow.state)
            assertEquals(0f, view.alpha, 0f)
            shadow.invokePreparedListener()
            assertTrue(player.isLooping)
            assertTrue(player.isPlaying)
            assertEquals(0f, shadow.leftVolume, 0f)
            assertEquals(0f, shadow.rightVolume, 0f)
            shadow.invokeInfoListener(MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START, 0)
            assertEquals(1f, view.alpha, 0f)
            owner.registry.currentState = Lifecycle.State.STARTED
            assertEquals(ShadowMediaPlayer.State.END, shadow.state) // Synchronous, without a Compose frame.
            view.release()
            assertEquals(ShadowMediaPlayer.State.END, shadow.state)
            assertEquals(0f, view.alpha, 0f)
            assertNull(view.surfaceTextureListener)
            view.release() // Detach and Compose disposal may both release the view.
            texture.release()
        }
    }

    @Test fun leavingDuringPreparationCannotStartAReleasedPlayer() {
        lateinit var player: MediaPlayer
        lateinit var shadow: ShadowMediaPlayer
        configurePlayer { p, s -> player = p; shadow = s }
        compose.runOnUiThread {
            val texture = SurfaceTexture(0)
            val view = WelcomeVideoView(compose.activity) { fail("Unexpected media failure") }
            view.onSurfaceTextureAvailable(texture, 360, 800)
            val callback = shadow.onPreparedListener
            view.release()
            callback.onPrepared(player)
            assertEquals(ShadowMediaPlayer.State.END, shadow.state)
            assertEquals(0f, view.alpha, 0f)
            texture.release()
        }
    }

    @Test fun decodeFailureReleasesMediaAndRevealsPoster() {
        lateinit var shadow: ShadowMediaPlayer
        configurePlayer { _, s -> shadow = s }
        compose.runOnUiThread {
            var failures = 0
            val texture = SurfaceTexture(0)
            val view = WelcomeVideoView(compose.activity) { failures++ }
            view.onSurfaceTextureAvailable(texture, 360, 800)
            shadow.invokeErrorListener(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
            assertEquals(1, failures)
            assertEquals(ShadowMediaPlayer.State.END, shadow.state)
            assertEquals(0f, view.alpha, 0f)
            view.release()
            texture.release()
        }
    }

    @Test fun playbackStopsForBackgroundLifecycleOverlaysAndReducedMotion() {
        lateinit var owner: TestOwner
        compose.runOnUiThread { owner = TestOwner().apply { registry.currentState = Lifecycle.State.RESUMED } }
        val allowed = mutableStateOf(true)
        val motion = mutableStateOf(true)
        compose.setContent {
            CleanCityTheme(darkTheme = false) {
                CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalCityMotion provides motion.value) {
                    WelcomeBackdrop(Modifier.fillMaxSize(), allowed.value)
                }
            }
        }
        compose.onNodeWithTag("welcome_video_surface").assertExists()
        compose.runOnIdle { allowed.value = false }
        compose.onNodeWithTag("welcome_video_surface").assertDoesNotExist()
        compose.onNodeWithTag("welcome_video_poster").assertIsDisplayed()
        compose.runOnIdle { allowed.value = true }
        compose.onNodeWithTag("welcome_video_surface").assertExists()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.onNodeWithTag("welcome_video_surface").assertDoesNotExist()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.onNodeWithTag("welcome_video_surface").assertExists()
        compose.runOnIdle { motion.value = false }
        compose.onNodeWithTag("welcome_video_surface").assertDoesNotExist()
        compose.onNodeWithTag("welcome_video_poster").assertIsDisplayed()
    }

    @Test fun welcomeCanPauseVideoAndEnterWithoutWaitingForPlayback() {
        val loggedIn = MutableStateFlow(false)
        val model = object : CityModel by PreviewCityModel() {
            override val isLoggedIn = loggedIn
            override fun loginWithDemoCitizen() { loggedIn.value = true }
        }
        compose.setContent {
            CleanCityTheme(darkTheme = false) { LoginScreen(model) }
        }
        compose.onNodeWithContentDescription("Pause background").performClick()
        compose.onNodeWithTag("welcome_video_surface").assertDoesNotExist()
        compose.onNodeWithContentDescription("Play background").performClick()
        compose.onNodeWithTag("welcome_video_surface").assertExists()
        compose.onNodeWithText("Get started").assertIsDisplayed().performClick()
        compose.onNodeWithTag("welcome_video_surface").assertDoesNotExist()
        assertTrue(loggedIn.value)
    }

    private class TestOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }
}
