package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.theme.CityColors
import com.example.util.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CollectionWeek(pickups: List<PickupLogEntity>) {
    val locale = Locale.forLanguageTag(appStrings().languageCode.lowercase())
    val now = System.currentTimeMillis()
    CityPanel {
        Text(cityText("thisWeek"), style = MaterialTheme.typography.labelMedium, color = CityColors.muted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (6 downTo 0).forEach { ago ->
                val day = CityFixtures.daysAgo(ago, now)
                val log = DemoScenario.today(pickups, day).firstOrNull()
                val done = log?.status == "COLLECTED"
                val label = DemoScenario.date(day) + " · " + if (done) cityText("collected") else if (log == null) cityText("remaining") else cityText("exceptions")
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = label },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(SimpleDateFormat("EEEEE", locale).format(day), style = MaterialTheme.typography.labelSmall, color = CityColors.muted)
                    Box(Modifier.size(28.dp).background(if (done) CityColors.primary else CityColors.soft, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(if (done) Icons.Default.Check else Icons.Default.Remove, null, Modifier.size(16.dp),
                            if (done) CityColors.onForest else CityColors.muted)
                    }
                }
            }
        }
    }
}
