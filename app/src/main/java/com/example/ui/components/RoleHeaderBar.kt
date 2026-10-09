package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DemoScenario
import com.example.ui.Appearance
import com.example.ui.UserRole
import com.example.ui.theme.CityColors
import com.example.util.appStrings
import com.example.util.cityText

@Composable
fun roleLabel(role: UserRole): String {
    val s = appStrings()
    return when (role) {
        UserRole.CITIZEN -> s.roleCitizen
        UserRole.COLLECTOR -> s.roleCollector
        UserRole.DRIVER -> s.roleDriver
        UserRole.SUPERVISOR -> s.roleSupervisor
        UserRole.MUNICIPAL_ADMIN -> s.roleAdmin
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoleHeaderBar(currentRole: UserRole, selectedLanguage: String, onRoleSelected: (UserRole) -> Unit,
    onLanguageSelected: (String) -> Unit, statusBanner: String? = null, onDismissBanner: () -> Unit = {},
    residentName: String? = null, onLogout: (() -> Unit)? = null, wardName: String? = null,
    appearance: Appearance = Appearance.LIGHT, onAppearance: (Appearance) -> Unit = {}) {
    var rolesOpen by remember { mutableStateOf(false) }
    var accountOpen by remember { mutableStateOf(false) }
    val s = appStrings()
    Surface(color = CityColors.canvas) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = CityColors.forest, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Eco, null, Modifier.padding(9.dp).size(22.dp), CityColors.lime)
                }
                Column(Modifier.weight(1f)) {
                    Text("CleanCity", style = MaterialTheme.typography.titleLarge)
                    Text(wardName ?: DemoScenario.ward, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall, color = CityColors.muted)
                }
                IconButton(onClick = { accountOpen = true }, Modifier.testTag("account_preferences")) {
                    Icon(Icons.Default.Tune, cityText("account"))
                }
            }
            TextButton(onClick = { rolesOpen = true }, modifier = Modifier.heightIn(min = 48.dp).testTag("role_switcher_button"),
                contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text(roleLabel(currentRole), style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Default.ExpandMore, null, Modifier.padding(start = 4.dp).size(20.dp))
                Spacer(Modifier.weight(1f))
                Text(cityText("demo"), style = MaterialTheme.typography.labelMedium, color = CityColors.muted)
            }
        }
    }
    if (rolesOpen) CitySheet(s.switchPerspective, { rolesOpen = false }) {
        UserRole.entries.forEach { role ->
            CityRecord(roleLabel(role), if (role == currentRole) cityText("completed") else cityText("explore"),
                when (role) { UserRole.CITIZEN -> Icons.Default.Home; UserRole.COLLECTOR -> Icons.Default.Recycling
                    UserRole.DRIVER -> Icons.Default.LocalShipping; UserRole.SUPERVISOR -> Icons.Default.Groups; else -> Icons.Default.Insights },
                { onRoleSelected(role); rolesOpen = false }) {
                if (role == currentRole) Icon(Icons.Default.CheckCircle, null, tint = CityColors.primary)
            }
        }
        DemoNote()
    }
    if (accountOpen) CitySheet(cityText("account"), { accountOpen = false }) {
        if (!residentName.isNullOrBlank()) Text(residentName, style = MaterialTheme.typography.titleLarge)
        SectionTitle(cityText("appearance"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Appearance.entries.forEach { value -> ChoiceChip(cityText(value.name.lowercase()), appearance == value, { onAppearance(value) }) }
        }
        SectionTitle(s.preferredLanguageLabel)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoScenario.languages.forEach { lang -> ChoiceChip(com.example.util.getAppStrings(lang).languageName, selectedLanguage == lang, { onLanguageSelected(lang) }) }
        }
        DemoNote()
        if (onLogout != null) OutlinedButton(onClick = { accountOpen = false; onLogout() }, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("btn_logout")) { Text(s.logout) }
    }
}

