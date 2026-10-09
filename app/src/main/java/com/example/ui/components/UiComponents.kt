package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.CityModel
import com.example.ui.theme.*
import com.example.util.appStrings
import com.example.util.cityText

@Composable
fun ScreenHeading(title: String, subtitle: String = "", modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = CityColors.ink, modifier = Modifier.semantics { heading() })
        if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = CityColors.muted)
    }
}
@Composable
fun CityPage(title: String, subtitle: String = "", modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { ScreenHeading(title, subtitle) }
        content()
    }
}
@Composable
fun CityPanel(modifier: Modifier = Modifier, color: Color = CityColors.surface, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), color = color, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, CityColors.line)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}
@Composable
fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge, color = CityColors.ink)
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}
@Composable
fun SectionHeading(number: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = CityColors.positiveBg, shape = RoundedCornerShape(10.dp)) {
            Text("%02d".format(number), Modifier.padding(10.dp), style = MaterialTheme.typography.labelLarge, color = CityColors.primary)
        }
        Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
    }
}
@Composable
fun CityButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    loading: Boolean = false, icon: ImageVector? = null) {
    Button(onClick = onClick, enabled = enabled && !loading, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(16.dp)) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        else if (icon != null) Icon(icon, null, Modifier.size(20.dp))
        if (loading || icon != null) Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
@Composable
fun CityField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text, multiline: Boolean = false, error: Boolean = false, enabled: Boolean = true) {
    OutlinedTextField(value, onValueChange, modifier.fillMaxWidth(), label = { Text(label) },
        singleLine = !multiline, minLines = if (multiline) 3 else 1, isError = error, enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard), shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = CityColors.surface, focusedContainerColor = CityColors.surface))
}
@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    FilterChip(selected, onClick, { Text(label) }, modifier.heightIn(min = 48.dp),
        leadingIcon = if (selected || icon != null) { { Icon(if (selected) Icons.Default.Check else icon!!, null, Modifier.size(18.dp)) } } else null,
        shape = RoundedCornerShape(14.dp))
}
@Composable
fun WorkspaceTabs(labels: List<String>, icons: List<ImageVector>, selectedIndex: Int, onSelect: (Int) -> Unit, tagPrefix: String) {
    if (LocalDensity.current.fontScale <= 1.3f) {
        TabRow(selectedTabIndex = selectedIndex, containerColor = CityColors.canvas, contentColor = CityColors.primary,
            divider = { HorizontalDivider(color = CityColors.line) }) {
            labels.forEachIndexed { index, label ->
                Tab(selected = selectedIndex == index, onClick = { onSelect(index) },
                    modifier = Modifier.heightIn(min = 52.dp).testTag(tagPrefix + "_" + index)) {
                    Text(label, Modifier.padding(horizontal = 4.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.labelLarge, maxLines = 2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        return
    }
    LazyRow(Modifier.fillMaxWidth().semantics { selectableGroup() }, horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
        itemsIndexed(labels) { index, label ->
            ChoiceChip(label, selectedIndex == index, { onSelect(index) }, Modifier.testTag(tagPrefix + "_" + index), icons.getOrNull(index))
        }
    }
}
@Composable
fun StatusBadge(label: String, tone: String = "positive", modifier: Modifier = Modifier) {
    val colors = when (tone) {
        "warning" -> CityColors.warning to CityColors.warningBg
        "error" -> CityColors.error to CityColors.errorBg
        "blue" -> CityColors.blue to CityColors.blueBg
        else -> CityColors.primary to CityColors.positiveBg
    }
    Surface(modifier, color = colors.second, shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).background(colors.first, RoundedCornerShape(3.dp)))
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.first)
        }
    }
}
@Composable
fun Metric(value: String, label: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Default.Insights) {
    Column(modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(22.dp), tint = CityColors.primary)
        Text(value, style = MaterialTheme.typography.displayMedium, color = CityColors.ink)
        Text(label, style = MaterialTheme.typography.bodySmall, color = CityColors.muted)
    }
}
@Composable
fun MetricPair(first: String, firstLabel: String, second: String, secondLabel: String) {
    CityPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Metric(first, firstLabel, Modifier.weight(1f))
            Metric(second, secondLabel, Modifier.weight(1f), Icons.Default.Eco)
        }
    }
}
@Composable
fun CityRecord(title: String, detail: String, icon: ImageVector = Icons.Default.Place,
    onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = CityColors.soft, shape = RoundedCornerShape(12.dp)) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(22.dp), CityColors.primary) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = CityColors.ink)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = CityColors.muted)
            }
            trailing()
        }
    }
    if (onClick != null) Surface(onClick = onClick, color = CityColors.surface, shape = RoundedCornerShape(14.dp), content = content)
    else Surface(color = CityColors.surface, shape = RoundedCornerShape(14.dp), content = content)
}
@Composable
fun EmptyState(title: String, icon: ImageVector = Icons.Default.Spa, modifier: Modifier = Modifier) {
    CityPanel(modifier, CityColors.soft) {
        Icon(icon, null, Modifier.size(36.dp), CityColors.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, color = CityColors.ink)
    }
}
@Composable
fun DemoNote(text: String = cityText("demoNotice")) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.Info, null, Modifier.size(18.dp), CityColors.muted)
        Text(text, style = MaterialTheme.typography.bodySmall, color = CityColors.muted)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CityColors.surface, contentColor = CityColors.ink) {
        Column(Modifier.fillMaxWidth().weight(1f, false).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle(title, appStrings().close, onDismiss)
            content()
        }
    }
}
@Composable
fun OperationFeedback(viewModel: CityModel, key: String, onSuccess: () -> Unit = {}) {
    val operations by viewModel.operations.collectAsStateWithLifecycle()
    val state = operations[key]
    val latestSuccess by rememberUpdatedState(onSuccess)
    LaunchedEffect(state) {
        if (state?.done == true) { viewModel.consumeOperation(key); latestSuccess() }
    }
    if (state?.error != null) {
        Surface(color = CityColors.errorBg, shape = RoundedCornerShape(12.dp)) {
            Text(state.error!!, Modifier.padding(14.dp).semantics { liveRegion = LiveRegionMode.Polite }, color = CityColors.error)
        }
    }
}
@Composable
fun AdaptiveColumns(primary: @Composable () -> Unit, secondary: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 800.dp) Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(20.dp)) { primary() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) { secondary() }
        } else Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { primary(); secondary() }
    }
}
@Composable
fun ProgressLine(progress: Float) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(220), label = "Progress")
    LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth().height(8.dp),
        color = CityColors.primary, trackColor = CityColors.soft)
}

