package com.psychoai.app.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.psychoai.app.model.ChatMessage
import com.psychoai.app.model.MessageSender
import com.psychoai.app.model.TradeTag
import com.psychoai.app.model.InsightCard
import com.psychoai.app.ui.theme.*
import com.psychoai.app.viewmodel.AuthViewModel
import com.psychoai.app.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Logout

@Composable
fun ChatScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
    ) {
        // Top App Bar
        ChatTopBar(
            onMenuClick = { /* drawer */ },
            onSettingsClick = { /* settings */ },
            onAvatarClick = { authViewModel.logout(); onLogout() }
        )

        // Session date
        Text(
            text = "SESSION: ${uiState.sessionDate}",
            color = TextHint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // Messages list
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

            // Typing indicator
            if (uiState.isTyping) {
                item {
                    TypingIndicator()
                }
            }

            // Insight banner
            if (uiState.hasInsight && !uiState.isTyping) {
                item {
                    InsightBanner(message = uiState.insightMessage)
                }
            }
        }

        // Quick action chips
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

        // Input bar
        ChatInputBar(
            value = inputText,
            onValueChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    chatViewModel.sendMessage(inputText)
                    inputText = ""
                }
            },
            onAttach = { /* file picker */ }
        )
    }
}

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
        // Menu icon
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = TextPrimary
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logo + name
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

        // Settings
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = TextSecondary
            )
        }

        // Avatar (tap to logout)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(SurfaceMedium)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = "Logout",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // AI avatar
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
            // Main bubble
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

                    // Trade tags
                    if (message.tradeTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            message.tradeTags.forEach { tag ->
                                TradeTagChip(tag = tag)
                            }
                        }
                    }

                    // Insight card embedded in AI bubble
                    message.insightCard?.let { card ->
                        Spacer(modifier = Modifier.height(10.dp))
                        EmbeddedInsightCard(card = card)
                    }

                    // Italic note for AI
                    if (!isUser && message.insightCard != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notice how your heart rate data spiked 30% right before hitting the 'Buy' button. You weren't trading the chart; you were trading your emotions.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Timestamp
            Text(
                text = message.timestamp,
                color = TextHint,
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
                val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "typing")

                listOf(0, 150, 300).forEachIndexed { index, delayMs ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(
                                durationMillis = 600,
                                delayMillis = delayMs
                            ),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
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
        // Attach button
        IconButton(
            onClick = onAttach,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AttachFile,
                contentDescription = "Attach",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Text input
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

        // Send button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Teal)
                .clickable { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
