package com.psychoai.app.ui.screens.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.psychoai.app.api.ChatSession
import com.psychoai.app.api.RetrofitClient
import com.psychoai.app.model.*
import com.psychoai.app.ui.theme.*
import com.psychoai.app.viewmodel.AuthViewModel
import com.psychoai.app.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val uiState       by chatViewModel.uiState.collectAsState()
    val listState      = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context        = LocalContext.current
    var inputText      by remember { mutableStateOf("") }

    // ── Drawer state ───────────────────────────────────────────
    var showHistory      by remember { mutableStateOf(false) }
    var historyLoading   by remember { mutableStateOf(false) }
    var historyError     by remember { mutableStateOf<String?>(null) }
    var chatSessions     by remember { mutableStateOf<List<ChatSession>>(emptyList()) }
    // Which session card is loading (spinner while fetching messages)
    var loadingSessionId by remember { mutableStateOf<String?>(null) }

    // ── File picker ────────────────────────────────────────────
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val fileName = context.contentResolver
            .query(uri, null, null, null, null)
            ?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (idx >= 0) cursor.getString(idx) else null
            } ?: uri.lastPathSegment ?: "file"

        val valid = fileName.endsWith(".csv", true) ||
                fileName.endsWith(".xlsx", true) ||
                fileName.endsWith(".xls", true)
        if (valid) {
            chatViewModel.sendMessage(
                "I have uploaded my trade journal: $fileName. Please analyze my trading patterns."
            )
            Toast.makeText(context, "Trade log uploaded. Plutus is analyzing...", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Please upload a CSV or Excel file only", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-scroll to bottom
    LaunchedEffect(uiState.messages.size, uiState.isTyping) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    (uiState.messages.size - 1 + if (uiState.isTyping) 1 else 0).coerceAtLeast(0)
                )
            }
        }
    }

    // ── Load sessions when drawer opens ───────────────────────
    fun loadSessions() {
        showHistory   = true
        historyLoading = true
        historyError   = null
        coroutineScope.launch {
            try {
                val uid      = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
                val response = RetrofitClient.api.getChatSessions(uid)
                if (response.isSuccessful) {
                    chatSessions = response.body()?.sessions ?: emptyList()
                } else {
                    historyError = "Could not load history (${response.code()})"
                }
            } catch (e: Exception) {
                historyError = "Network error: ${e.message}"
            }
            historyLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Main chat column ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundBlack)
                .imePadding()
        ) {
            ChatTopBar(
                sessionTitle  = uiState.sessionTitle,
                onMenuClick   = { loadSessions() },
                onSettingsClick = {
                    Toast.makeText(context, "Settings coming soon", Toast.LENGTH_SHORT).show()
                },
                onAvatarClick = { authViewModel.logout(); onLogout() }
            )

            Text(
                text      = "SESSION: ${uiState.sessionDate}",
                color     = TextHint,
                fontSize  = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                textAlign = TextAlign.Center
            )

            LazyColumn(
                state            = listState,
                modifier         = Modifier.weight(1f).fillMaxWidth(),
                contentPadding   = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        MessageBubble(message = message)
                    }
                }
                if (uiState.isTyping) item { TypingIndicator() }
                if (uiState.hasInsight && !uiState.isTyping) {
                    item { InsightBanner(message = uiState.insightMessage) }
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.quickActions) { action ->
                    QuickActionChip(label = action.label) {
                        chatViewModel.sendQuickAction(action.label)
                    }
                }
            }

            ChatInputBar(
                value         = inputText,
                onValueChange = { inputText = it },
                onSend        = {
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
            enter   = slideInHorizontally { -it },
            exit    = slideOutHorizontally { -it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showHistory = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.85f)
                        .background(SurfaceDark)
                        .clickable { /* absorb clicks */ }
                ) {

                    // ── Drawer header ──────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceMedium)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = "History",
                            color      = TextPrimary,
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                chatViewModel.startNewSession()
                                showHistory = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Purple),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("New Chat", color = Color.White, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { showHistory = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                        }
                    }

                    // ── Drawer body ────────────────────────────
                    when {
                        historyLoading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Purple)
                            }
                        }

                        historyError != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(historyError ?: "Error", color = ErrorRed, fontSize = 14.sp,
                                    textAlign = TextAlign.Center)
                            }
                        }

                        chatSessions.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("💬", fontSize = 40.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Text("No history yet", color = TextPrimary, fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Your conversations will appear here.\nTap + New Chat to start.",
                                        color = TextSecondary, fontSize = 13.sp,
                                        textAlign = TextAlign.Center, lineHeight = 19.sp
                                    )
                                }
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                items(chatSessions, key = { it.sessionId }) { session ->
                                    SessionHistoryCard(
                                        session          = session,
                                        isActive         = session.sessionId == uiState.currentSessionId,
                                        isLoading        = loadingSessionId == session.sessionId,
                                        onClick          = {
                                            if (session.sessionId == uiState.currentSessionId) {
                                                // Already in this session — just close
                                                showHistory = false
                                                return@SessionHistoryCard
                                            }
                                            // Fetch full session messages then restore
                                            loadingSessionId = session.sessionId
                                            coroutineScope.launch {
                                                try {
                                                    val resp = RetrofitClient.api.getSession(session.sessionId)
                                                    if (resp.isSuccessful && resp.body() != null) {
                                                        chatViewModel.restoreSession(resp.body()!!)
                                                    } else {
                                                        // Restore from what we already have in the card
                                                        chatViewModel.restoreSession(session)
                                                    }
                                                } catch (e: Exception) {
                                                    chatViewModel.restoreSession(session)
                                                }
                                                loadingSessionId = null
                                                showHistory = false
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Session history card ───────────────────────────────────────
@Composable
private fun SessionHistoryCard(
    session:   ChatSession,
    isActive:  Boolean,
    isLoading: Boolean,
    onClick:   () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isActive) Purple.copy(alpha = 0.08f) else Color.Transparent
            )
            .clickable(enabled = !isLoading) { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Purple.copy(alpha = if (isActive) 0.25f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color    = Purple,
                    strokeWidth = 2.dp
                )
            } else {
                Text("💬", fontSize = 20.sp)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = session.title,
                color      = if (isActive) Purple else TextPrimary,
                fontSize   = 14.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            // Show last message preview if messages are available
            val preview = session.messages.lastOrNull { it.role == "assistant" }
                ?.content?.take(80) ?: ""
            if (preview.isNotBlank()) {
                Text(
                    text     = preview,
                    color    = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(3.dp))
            }
            Text(
                text     = session.date,
                color    = TextHint,
                fontSize = 11.sp
            )
        }

        if (isActive) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Purple)
            )
        }
    }

    HorizontalDivider(
        color     = InputBorder,
        thickness = 0.5.dp,
        modifier  = Modifier.padding(horizontal = 16.dp)
    )
}

// ── Top app bar ────────────────────────────────────────────────
@Composable
private fun ChatTopBar(
    sessionTitle:    String,
    onMenuClick:     () -> Unit,
    onSettingsClick: () -> Unit,
    onAvatarClick:   () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Default.Menu, contentDescription = "History", tint = TextPrimary)
        }

        Spacer(Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Purple),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚡", fontSize = 13.sp)
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    text       = "Psycho AI",
                    color      = Purple,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            // Show session title below app name
            if (sessionTitle.isNotBlank() && sessionTitle != "New Chat") {
                Text(
                    text      = sessionTitle,
                    color     = TextHint,
                    fontSize  = 10.sp,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    modifier  = Modifier.widthIn(max = 180.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
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
                Text("🤖", fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart    = 16.dp,
                            topEnd      = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd   = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) UserBubble else AiBubble)
                    .padding(12.dp, 10.dp)
            ) {
                Column {
                    Text(
                        text       = message.text,
                        color      = TextPrimary,
                        fontSize   = 14.sp,
                        lineHeight = 20.sp
                    )
                    if (message.tradeTags.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            message.tradeTags.forEach { TradeTagChip(it) }
                        }
                    }
                }
            }
            Text(
                text     = message.timestamp,
                color    = TextHint,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

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
            Text("${tag.label}: ", color = TextHint, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(tag.value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
        ) { Text("🤖", fontSize = 14.sp) }
        Spacer(Modifier.width(8.dp))
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
                        targetValue  = 1f,
                        animationSpec = infiniteRepeatable(
                            animation  = tween(600, delayMillis = delayMs),
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
                Spacer(Modifier.width(4.dp))
                Text(
                    text          = "BUILDING MINDSET MAP...",
                    color         = TextHint,
                    fontSize      = 10.sp,
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
        ) { Text("📊", fontSize = 16.sp) }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Behavioral Insight Ready", color = TextPrimary, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold)
            Text(message, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Teal)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text("Audit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Quick action chip ──────────────────────────────────────────
@Composable
private fun QuickActionChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, InputBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Chat input bar ─────────────────────────────────────────────
@Composable
private fun ChatInputBar(
    value:         String,
    onValueChange: (String) -> Unit,
    onSend:        () -> Unit,
    onAttach:      () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAttach, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Outlined.AttachFile,
                contentDescription = "Attach trade journal",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceMedium)
                .border(1.dp, InputBorder, RoundedCornerShape(24.dp))
        ) {
            TextField(
                value         = value,
                onValueChange = onValueChange,
                placeholder   = {
                    Text("Ask Plutus or upload trade log", color = TextHint, fontSize = 13.sp)
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor        = TextPrimary,
                    unfocusedTextColor      = TextPrimary,
                    cursorColor             = Teal
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.width(8.dp))
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