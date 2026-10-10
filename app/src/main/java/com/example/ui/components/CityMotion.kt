package com.example.ui.components

import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CityColors

val LocalCityMotion = staticCompositionLocalOf { true }
enum class CityFeedback { SELECTION, CONFIRM }
val LocalCityFeedback = staticCompositionLocalOf<(CityFeedback) -> Unit> { {} }

@Composable
fun CityMotionProvider(content: @Composable () -> Unit) {
    val resolver = LocalContext.current.contentResolver
    val view = LocalView.current
    fun animationsEnabled() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    var enabled by remember(resolver) { mutableStateOf(animationsEnabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { enabled = animationsEnabled() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    val feedback = remember(view) { { kind: CityFeedback ->
        val effect = if (kind == CityFeedback.CONFIRM && Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
            else if (kind == CityFeedback.SELECTION) HapticFeedbackConstants.CLOCK_TICK
            else HapticFeedbackConstants.KEYBOARD_TAP
        // The platform respects the user's touch-feedback setting and device capabilities.
        view.performHapticFeedback(effect)
        Unit
    } }
    CompositionLocalProvider(LocalCityMotion provides enabled, LocalCityFeedback provides feedback, content = content)
}

@Composable
fun Modifier.cityPress(interactions: MutableInteractionSource): Modifier {
    val pressed by interactions.collectIsPressedAsState()
    val motion = LocalCityMotion.current
    val scale by animateFloatAsState(if (pressed && motion) .98f else 1f,
        if (motion) spring(dampingRatio = .85f, stiffness = 700f) else snap(), label = "Press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Saved per destination: a returned-to page is immediately readable, without replaying entrances. */
@Composable
fun arrivalProgress(order: Int = 0): State<Float> {
    val motion = LocalCityMotion.current
    var entered by rememberSaveable { mutableStateOf(false) }
    val progress = animateFloatAsState(if (entered || !motion) 1f else 0f,
        if (motion) tween(420, order.coerceIn(0, 3) * 40, FastOutSlowInEasing) else snap(), label = "Arrival")
    LaunchedEffect(Unit) { entered = true }
    return progress
}

@Composable
fun Arrival(order: Int = 0, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val progress by arrivalProgress(order)
    Box(modifier.graphicsLayer { alpha = progress; translationY = (1f - progress) * 14.dp.toPx() }) { content() }
}

@Composable
fun <T> CityTransition(target: T, modifier: Modifier = Modifier, direction: (T, T) -> Int = { _, _ -> 0 },
    content: @Composable (T) -> Unit) {
    val motion = LocalCityMotion.current
    AnimatedContent(target, modifier, transitionSpec = {
        if (!motion) EnterTransition.None togetherWith ExitTransition.None
        else {
            val sign = direction(initialState, targetState)
            (fadeIn(tween(150, 75)) + slideInHorizontally(spring(1f, 650f)) { sign * (it * .06f).toInt() }) togetherWith
                (fadeOut(tween(90)) + slideOutHorizontally(tween(150)) { -sign * (it * .03f).toInt() })
        }.using(SizeTransform(clip = false))
    }, label = "Destination") { content(it) }
}

@Composable
fun MotionNumber(value: Int, suffix: String = "", style: TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = CityColors.ink) {
    val motion = LocalCityMotion.current
    val number by animateIntAsState(value, if (motion) tween(420, easing = FastOutSlowInEasing) else snap(), label = "Updated value")
    Text(number.toString() + suffix, style = style, color = color)
}

@Composable
fun SuccessMark(modifier: Modifier = Modifier, color: Color = CityColors.primary) {
    val progress by arrivalProgress()
    Canvas(modifier.size(26.dp)) {
        val p = Path()
        val first = (progress / .38f).coerceIn(0f, 1f)
        val second = ((progress - .38f) / .62f).coerceIn(0f, 1f)
        p.moveTo(size.width * .18f, size.height * .52f)
        p.lineTo(size.width * (.18f + .23f * first), size.height * (.52f + .22f * first))
        if (second > 0f) p.lineTo(size.width * (.41f + .43f * second), size.height * (.74f - .49f * second))
        drawPath(p, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}
