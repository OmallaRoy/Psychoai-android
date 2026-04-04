package com.psychoai.app.ui.components

import android.R.attr.contentDescription
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psychoai.app.ui.theme.*
import com.psychoai.app.R

@Composable
fun PsychoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    isFocused: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    val borderColor = if (isFocused) Purple else InputBorder

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InputBackground)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = TextHint,
                    fontSize = 15.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = TextHint,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = TextHint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = Purple
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PsychoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = Purple
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 16.sp,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true
) {
    val bgColor = if (isDark) SurfaceDark else Color.White
    val textColor = if (isDark) TextPrimary else Color(0xFF1F1F1F)
    val borderColor = if (isDark) InputBorder else Color(0xFFDDDDDD)

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Google G logo using text approximation
            Image(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = "Google Sign-In",
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continue with Google",
                color = textColor,
                fontSize = 15.sp,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(
            modifier = Modifier.weight(1f),
            color = InputBorder
        )
        Text(
            text = "OR",
            color = TextHint,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelMedium
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = InputBorder
        )
    }
}

@Composable
fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message.isNotBlank(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(ErrorRed.copy(alpha = 0.15f))
                .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = message,
                color = ErrorRed,
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SuccessMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message.isNotBlank(),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SuccessGreen.copy(alpha = 0.15f))
                .border(1.dp, SuccessGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = message,
                color = SuccessGreen,
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PsychoLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Purple),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⚡",
            fontSize = 24.sp
        )
    }
}

@Composable
fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = TextPrimary,
        fontSize = 14.sp,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
    )
}
