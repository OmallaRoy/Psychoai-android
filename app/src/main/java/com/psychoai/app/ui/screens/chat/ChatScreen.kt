package com.psychoai.app.ui.screens.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.psychoai.app.api.RetrofitClient
import com.psychoai.app.api.UnifiedHistoryEntry
import com.psychoai.app.model.ChatMessage
import com.psychoai.app.model.InsightCard
import com.psychoai.app.model.MessageSender
import com.psychoai.app.model.TradeTag
import com.psychoai.app.ui.theme.AiBubble
import com.psychoai.app.ui.theme.BackgroundBlack
import com.psychoai.app.ui.theme.ErrorRed
import com.psychoai.app.ui.theme.InputBorder
import com.psychoai.app.ui.theme.Purple
import com.psychoai.app.ui.theme.SurfaceDark
import com.psychoai.app.ui.theme.SurfaceLight
import com.psychoai.app.ui.theme.SurfaceMedium
import com.psychoai.app.ui.theme.Teal
import com.psychoai.app.ui.theme.TextHint
import com.psychoai.app.ui.theme.TextPrimary
import com.psychoai.app.ui.theme.TextSecondary
import com.psychoai.app.ui.theme.UserBubble
import com.psychoai.app.viewmodel.AuthViewModel
import com.psychoai.app.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }

    // ── History drawer state ───────────────────────────────────
    var showHistory by remember { mutableStateOf(false) }
    var historyLoading by remember { mutableStateOf(false) }
    var historyError by remember { mutableStateOf<String?>(null) }
    var unifiedHistory by remember { mutableStateOf<List<UnifiedHistoryEntry>>(emptyList()) }
    var selectedEntry by remember { mutableStateOf<UnifiedHistoryEntry?>(null) }

    // ── File picker — CSV and Excel only ──────────────────────
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = context.contentResolver
                .query(uri, null, null, null, null)
                ?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(
                        android.provider.OpenableColumns.DISPLAY_NAME
                    )
                    cursor.moveToFirst()
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                } ?: uri.lastPathSegment ?: "file"

            val isValid = fileName.endsWith(".csv", ignoreCase = true)
                    || fileName.endsWith(".xlsx", ignoreCase = true)
                    || fileName.endsWith(".xls", ignoreCase = true)

            if (isValid) {
                chatViewModel.sendMessage(
                    "I have uploaded my trade journal: $fileName. Please analyze my trading patterns."
                )
                Toast.makeText(
                    context,
                    "Trade log uploaded. Plutus is analyzing...",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Please upload a CSV or Excel file only",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(uiState.messages.size, uiState.isTyping) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    (uiState.messages.size - 1 + if (uiState.isTyping) 1 else 0)
                        .coerceAtLeast(0)
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Main chat content ──────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundBlack)
                .imePadding()
        ) {
            ChatTopBar(
                onMenuClick = {
                    showHistory = true
                    historyLoading = true
                    historyError = null

                    // ── UPDATED: uses unified history endpoint ─
                    coroutineScope.launch {
                        try {
                            val uid = FirebaseAuth.getInstance()
                                .currentUser?.uid ?: "anonymous"

                            // Use new unified history endpoint
                            val response = RetrofitClient.api.getFullHistory(uid)
                            if (response.isSuccessful) {
                                val items = response.body()?.items ?: emptyList()
                                unifiedHistory = items.map { item ->
                                    UnifiedHistoryEntry(
                                        type         = item.type,
                                        date         = item.date,
                                        timestamp    = 0L,
                                        pattern      = item.pattern,
                                        firstMessage = item.preview,
                                        snippet      = item.preview,
                                        confidence   = item.confidence
                                    )
                                }
                            } else {
                                historyError = "Could not load history (${response.code()})"
                            }
                        } catch (e: Exception) {
                            historyError = "Network error: ${e.message}"
                        }
                        historyLoading = false
                    }
                },
                onSettingsClick = {
                    Toast.makeText(context, "Settings coming soon", Toast.LENGTH_SHORT).show()
                },
                onAvatarClick = {
                    authViewModel.logout()
                    onLogout()
                }
            )

            Text(
                text = "SESSION: ${uiState.sessionDate}",
                color = TextHint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                textAlign = TextAlign.Center
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        MessageBubble(message = message)
                    }
                }
                if (uiState.isTyping) {
                    item { TypingIndicator() }
                }
                if (uiState.hasInsight && !uiState.isTyping) {
                    item { InsightBanner(message = uiState.insightMessage) }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.quickActions) { action ->
                    QuickActionChip(
                        label = action.label,
                        onClick = { chatViewModel.sendQuickAction(action.label) }
                    )
                }
            }

            ChatInputBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        chatViewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                onAttach = { filePicker.launch("*/*") }
            )
        }

        // ── History drawer overlay ─────────────────────────────
        AnimatedVisibility(
            visible = showHistory,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showHistory = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.82f)
                        .background(SurfaceDark)
                        .clickable { }
                ) {
                    // Drawer header with New Chat button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceMedium)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "History",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        // New Chat button
                        Button(
                            onClick = {
                                chatViewModel.startNewSession()
                                showHistory = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Purple
                            ),
                            contentPadding = PaddingValues(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New Chat",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = { showHistory = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextPrimary
                            )
                        }
                    }

                    // Drawer body
                    when {
                        historyLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Purple)
                            }
                        }

                        historyError != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = historyError ?: "Error",
                                    color = ErrorRed,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        unifiedHistory.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "🤖", fontSize = 40.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No history yet",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Your conversations and coaching sessions will appear here.",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(unifiedHistory) { entry ->
                                    UnifiedHistoryItem(
                                        entry = entry,
                                        onClick = {
                                            selectedEntry = entry
                                            showHistory = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Detail dialog when history item tapped ─────────────
        selectedEntry?.let { entry ->
            AlertDialog(
                onDismissRequest = { selectedEntry = null },
                containerColor = SurfaceDark,
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (entry.type == "coaching")
                                    patternEmoji(entry.pattern) else "💬",
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (entry.type == "coaching")
                                    entry.pattern else "Chat Session",
                                color = Purple,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = entry.date,
                            color = TextHint,
                            fontSize = 12.sp
                        )
                    }
                },
                text = {
                    Column {
                        if (entry.type == "chat" && entry.firstMessage.isNotBlank()) {
                            Text(
                                text = "You said:",
                                color = TextHint,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${entry.firstMessage}\"",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Plutus replied:",
                                color = TextHint,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = entry.snippet.ifBlank {
                                "No content available for this session."
                            },
                            color = TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedEntry = null }) {
                        Text("Close", color = Purple)
                    }
                }
            )
        }
    }
}

// ── Helper: pattern to emoji ───────────────────────────────────
private fun patternEmoji(pattern: String): String = when {
    pattern.contains("Revenge",    ignoreCase = true) -> "😤"
    pattern.contains("FOMO",       ignoreCase = true) -> "😰"
    pattern.contains("Oversized",  ignoreCase = true) -> "📈"
    pattern.contains("Stop Loss",  ignoreCase = true) -> "🛑"
    pattern.contains("Impulsive",  ignoreCase = true) -> "⚡"
    pattern.contains("Winner",     ignoreCase = true) -> "✂️"
    pattern.contains("Loser",      ignoreCase = true) -> "🔒"
    pattern.contains("No Mistake", ignoreCase = true) -> "✅"
    else -> "🤖"
}

// ── Unified history list item ──────────────────────────────────
@Composable
private fun UnifiedHistoryItem(
    entry: UnifiedHistoryEntry,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon — coaching vs chat
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (entry.type == "coaching")
                        Purple.copy(alpha = 0.15f)
                    else
                        Teal.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (entry.type == "coaching")
                    patternEmoji(entry.pattern)
                else "💬",
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (entry.type == "coaching") entry.pattern else "Chat",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                // Confidence badge for coaching sessions
                if (entry.type == "coaching" && entry.confidence > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Purple.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${(entry.confidence * 100).toInt()}%",
                            color = Purple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (entry.type == "chat" && entry.firstMessage.isNotBlank())
                    entry.firstMessage
                else
                    entry.snippet,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.date,
                color = TextHint,
                fontSize = 11.sp
            )
        }
    }

    HorizontalDivider(
        color = InputBorder,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

// ── Top app bar ────────────────────────────────────────────────
@Composable
private fun ChatTopBar(
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Session history",
                tint = TextPrimary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Purple),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚡", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Psycho AI",
                color = Purple,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = TextSecondary
            )
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(SurfaceMedium)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Logout",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Message bubble ─────────────────────────────────────────────
@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceMedium)
                    .border(1.dp, Purple.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🤖", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) UserBubble else AiBubble)
                    .padding(12.dp, 10.dp)
            ) {
                Column {
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    if (message.tradeTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            message.tradeTags.forEach { tag ->
                                TradeTagChip(tag = tag)
                            }
                        }
                    }

                    message.insightCard?.let { card ->
                        Spacer(modifier = Modifier.height(10.dp))
                        EmbeddedInsightCard(card = card)
                    }

                    if (!isUser && message.insightCard != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notice how this pattern appears when you deviate from your plan. Your behavior — not the market — drives these outcomes.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Text(
                text = message.timestamp,
                color = TextHint,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

// ── Trade tag chip ─────────────────────────────────────────────
@Composable
private fun TradeTagChip(tag: TradeTag) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceMedium)
            .border(1.dp, InputBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${tag.label}: ",
                color = TextHint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = tag.value,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Embedded insight card ──────────────────────────────────────
@Composable
private fun EmbeddedInsightCard(card: InsightCard) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceLight.copy(alpha = 0.6f))
            .border(1.dp, InputBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(text = "⚠️", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = card.title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = card.description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

// ── Typing indicator ───────────────────────────────────────────
@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SurfaceMedium)
                .border(1.dp, Purple.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🤖", fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(AiBubble)
                .padding(14.dp, 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "typing")
                listOf(0, 150, 300).forEachIndexed { index, delayMs ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 600, delayMillis = delayMs),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(TextSecondary.copy(alpha = alpha))
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "BUILDING MINDSET MAP...",
                    color = TextHint,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

// ── Insight banner ─────────────────────────────────────────────
@Composable
private fun InsightBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, InputBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Teal.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📊", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Behavioral Insight Ready",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Teal)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Audit",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Quick action chip ──────────────────────────────────────────
@Composable
private fun QuickActionChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, InputBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Chat input bar ─────────────────────────────────────────────
@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onAttach,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AttachFile,
                contentDescription = "Attach CSV or Excel trade journal",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceMedium)
                .border(1.dp, InputBorder, RoundedCornerShape(24.dp))
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = "Ask Plutus or upload trade log",
                        color = TextHint,
                        fontSize = 13.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Teal
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Teal)
                .clickable { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}