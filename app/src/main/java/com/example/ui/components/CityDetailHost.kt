@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.example.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CityColors
import com.example.util.appStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val LocalSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
private val LocalVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

@Composable
fun Modifier.cityShared(key: String): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val visibility = LocalVisibilityScope.current ?: return this
    if (!LocalCityMotion.current) return this
    return with(shared) {
        sharedBounds(rememberSharedContentState(key), visibility,
            boundsTransform = { _, _ -> spring(dampingRatio = 1f, stiffness = 500f) })
    }
}

/** List and detail live in the same composition, allowing bounds to connect in both directions. */
@Composable
fun CityDetailHost(selectedId: Int?, onClose: () -> Unit, modifier: Modifier = Modifier,
    canClose: Boolean = true, list: @Composable () -> Unit, detail: @Composable (Int) -> Unit) {
    val state = remember { SeekableTransitionState(selectedId) }
    val transition = rememberTransition(state, label = "Detail")
    val holder = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    val motion = LocalCityMotion.current
    val currentClose by rememberUpdatedState(onClose)
    val closeAllowed by rememberUpdatedState(canClose)
    LaunchedEffect(selectedId, motion) {
        if (motion) state.animateTo(selectedId) else state.snapTo(selectedId)
    }
    SharedTransitionLayout(modifier.fillMaxSize()) {
        val shared = this
        transition.AnimatedContent(transitionSpec = {
            if (!motion) EnterTransition.None togetherWith ExitTransition.None
            else (fadeIn(tween(180, 50)) togetherWith fadeOut(tween(130))).using(SizeTransform(clip = false))
        }) { id ->
            CompositionLocalProvider(LocalSharedScope provides shared, LocalVisibilityScope provides this) {
                holder.SaveableStateProvider(id ?: "list") {
                    if (id == null) list() else detail(id)
                }
            }
        }
    }
    PredictiveBackHandler(enabled = selectedId != null) { events ->
        try {
            events.collect { event ->
                if (motion && closeAllowed) state.seekTo(event.progress, targetState = null)
            }
            if (closeAllowed) currentClose()
            else state.animateTo(selectedId)
        } catch (cancelled: CancellationException) {
            scope.launch {
                if (motion) state.animateTo(selectedId) else state.snapTo(selectedId)
            }
            throw cancelled
        }
    }
}

@Composable
fun CityDetailPage(sharedKey: String, title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxSize().cityShared(sharedKey), color = CityColors.surface, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) { SectionTitle(title, appStrings().close, onClose) }
            Column(Modifier.weight(1f).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp), content = content)
        }
    }
}
