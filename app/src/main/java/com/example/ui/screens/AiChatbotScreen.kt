package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.CityModel
import com.example.ai.GroqCredentials
import com.example.ui.components.*
import com.example.ui.theme.CityColors
import com.example.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatbotScreen(viewModel: CityModel, modifier: Modifier = Modifier) {
    val s = appStrings()
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val busy by viewModel.isChatLoading.collectAsStateWithLifecycle()
    val error by viewModel.chatError.collectAsStateWithLifecycle()
    val configured by viewModel.chatConfigured.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    var connectionOpen by rememberSaveable { mutableStateOf(false) }
    var keyDraft by remember { mutableStateOf("") }
    var draft by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    val send: () -> Unit = {
        if (draft.isNotBlank() && !busy && configured) { viewModel.sendChatMessage(draft.trim()); draft = "" }
    }
    LaunchedEffect(messages.size, busy, error) {
        if (messages.isNotEmpty()) list.animateScrollToItem(messages.size + if (busy || error != null) 1 else 0)
    }
    Column(modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(color = CityColors.positiveBg, shape = RoundedCornerShape(18.dp)) { Icon(Icons.Default.AutoAwesome, null, Modifier.padding(16.dp).size(30.dp), CityColors.primary) }
                    ScreenHeading(cityText("assistant"), cityText("assistantIntro"))
                    StatusBadge(cityText(if (configured) "onlineAssistant" else "chatConnect"))
                    Text(cityText("chatPrivacy"), style = MaterialTheme.typography.bodySmall, color = CityColors.muted)
                    TextButton({ viewModel.clearOperation(); keyDraft = ""; connectionOpen = true }) {
                        Text(cityText(if (configured) "chatConnection" else "chatConnect"))
                    }
                    if (messages.isEmpty()) Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(s.promptPickupTimes, s.promptSegregation, s.promptPayFee, s.promptHomeCompost).forEachIndexed { i, prompt ->
                            SuggestionChip({ viewModel.sendChatMessage(prompt) }, { Text(cityText(listOf("pickupQuestion", "sortQuestion", "billQuestion", "compostQuestion")[i])) }, enabled = !busy && configured, modifier = Modifier.heightIn(min = 48.dp))
                        }
                    }
                }
            }
            itemsIndexed(messages) { _, message ->
                val user = message.sender == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
                    Surface(color = if (user) CityColors.primary else CityColors.surface,
                        contentColor = if (user) CityColors.onPrimary else CityColors.ink, shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(if (user) .88f else .96f)) {
                        SelectionContainer { Text(message.text, Modifier.padding(18.dp), style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }
            if (busy) item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Text(s.aiThinking, color = CityColors.muted)
            } }
            if (error != null) item { CityPanel { Text(error!!, color = CityColors.error); CityButton(cityText("retry"), viewModel::retryChat) } }
        }
        Surface(color = CityColors.surface, tonalElevation = 0.dp) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(draft, { draft = it.take(1500) }, Modifier.weight(1f), placeholder = { Text(cityText("askHint")) },
                    maxLines = 4, enabled = configured, shape = RoundedCornerShape(20.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }))
                FilledIconButton(send, enabled = draft.isNotBlank() && !busy && configured, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, s.send)
                }
            }
        }
    }
    if (connectionOpen) CitySheet(cityText("chatConnection"), { if (!operation.busy) { keyDraft = ""; connectionOpen = false } }) {
        Text(cityText("chatKeyHelp"), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(keyDraft, { keyDraft = it.trim().take(204) }, label = { Text("Groq API key") },
            modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !operation.busy,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        OperationFeedback(viewModel, "chatKey") { keyDraft = ""; connectionOpen = false }
        CityButton(cityText("chatSaveKey"), { viewModel.configureChatKey(keyDraft) },
            enabled = GroqCredentials.valid(keyDraft), loading = operation.busy)
        if (configured) TextButton({ viewModel.removeChatKey() }, enabled = !operation.busy) { Text(cityText("chatRemoveKey")) }
    }
}

