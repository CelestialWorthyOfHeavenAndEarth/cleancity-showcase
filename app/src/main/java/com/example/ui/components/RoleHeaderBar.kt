package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DemoScenario
import com.example.data.HouseholdEntity
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
    appearance: Appearance = Appearance.LIGHT, onAppearance: (Appearance) -> Unit = {},
    households: List<HouseholdEntity> = emptyList(), onHouseholdSelected: (Int) -> Unit = {}) {
    var rolesOpen by remember { mutableStateOf(false) }
    var accountOpen by remember { mutableStateOf(false) }
    var homesOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val s = appStrings()
    val largeText = LocalDensity.current.fontScale > 1.3f
    val roleControl: @Composable (Modifier) -> Unit = { modifier ->
        TextButton({ rolesOpen = true }, modifier.heightIn(min = 48.dp).testTag("role_switcher_button"), contentPadding = PaddingValues(4.dp)) {
            Text(if (currentRole == UserRole.MUNICIPAL_ADMIN) cityText("adminShort") else roleLabel(currentRole),
                style = MaterialTheme.typography.labelMedium, maxLines = 2)
            Icon(Icons.Default.ExpandMore, null, Modifier.size(16.dp))
        }
    }
    Surface(color = CityColors.canvas) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = CityColors.forest, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Eco, null, Modifier.padding(10.dp).size(22.dp), CityColors.onForest)
                }
                Column(Modifier.weight(1f)) {
                    Text("CleanCity", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text((wardName ?: DemoScenario.ward).substringBefore(" -"), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall, color = CityColors.muted)
                }
                if (!largeText) roleControl(Modifier.widthIn(max = 116.dp))
                IconButton({ accountOpen = true }, Modifier.testTag("account_preferences").size(48.dp)) {
                    Icon(Icons.Default.Tune, cityText("account"), Modifier.size(22.dp))
                }
            }
            if (largeText) roleControl(Modifier)
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
    }
    if (accountOpen) CitySheet(cityText("account"), { accountOpen = false }) {
        if (!residentName.isNullOrBlank()) Text(residentName, style = MaterialTheme.typography.titleLarge)
        if (households.isNotEmpty()) CityRecord(cityText("switchHome"), households.size.toString() + " · " + cityText("households"), Icons.Default.Home,
            { accountOpen = false; homesOpen = true }) { Icon(Icons.Default.ChevronRight, null) }
        SectionTitle(cityText("appearance"))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Appearance.entries.forEach { value -> ChoiceChip(cityText(value.name.lowercase()), appearance == value, { onAppearance(value) }) }
        }
        SectionTitle(s.preferredLanguageLabel)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DemoScenario.languages.forEach { lang -> ChoiceChip(com.example.util.getAppStrings(lang).languageName, selectedLanguage == lang, { onLanguageSelected(lang) }) }
        }
        if (onLogout != null) CityOutlineButton(onClick = { accountOpen = false; onLogout() }, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("btn_logout")) { Text(s.logout) }
    }
    if (homesOpen) CitySheet(cityText("switchHome"), { homesOpen = false }) {
        CityField(query, { query = it }, cityText("searchHomes"))
        households.filter { it.residentName.contains(query, true) || it.address.contains(query, true) }.forEach { home ->
            PersonRecord(home.residentName, home.ward.substringBefore(","), { onHouseholdSelected(home.id); homesOpen = false }) {
                if (home.residentName == residentName) Icon(Icons.Default.CheckCircle, null, tint = CityColors.primary)
                else Icon(Icons.Default.ChevronRight, null)
            }
        }
    }

}

