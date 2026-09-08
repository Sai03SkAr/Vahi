package com.vahi.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahi.app.ui.components.WindowClass
import com.vahi.app.ui.components.windowClass
import com.vahi.app.ui.theme.*

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit = {}
) {
    when (windowClass()) {
        WindowClass.Expanded -> AuthTabletLayout(onLoginSuccess)
        else -> AuthFallbackLayout()
    }
}

/* ---------------- TABLET ---------------- */

@Composable
private fun AuthTabletLayout(
    onLoginSuccess: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {

        /* LEFT — AUTH */
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            AuthCard(onLoginSuccess)
        }

        /* RIGHT — BRAND */
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(VahiCardLavender.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Vahi",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = VahiAccentPurple
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Your Digital Notebook",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

/* ---------------- AUTH CARD ---------------- */

@Composable
private fun AuthCard(
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .width(380.dp)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Username field
        VahiStyledField(
            value = username,
            onValueChange = { username = it },
            label = "Username",
            placeholder = "Username"
        )

        // Email field
        VahiStyledField(
            value = email,
            onValueChange = { email = it },
            label = "E-mail",
            placeholder = "example@mail.com"
        )

        // Password field
        VahiStyledField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            placeholder = "Password",
            isPassword = true
        )

        // Password confirmation
        VahiStyledField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Password confirmation",
            placeholder = "Password",
            isPassword = true
        )

        Spacer(Modifier.height(16.dp))

        // Login button
        Button(
            onClick = onLoginSuccess,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VahiAccentPurple
            )
        ) {
            Text(
                "Login",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))

        // Divider with gradient effect
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = VahiInputBorder.copy(alpha = 0.5f)
        )


        // Continue with Google
        OutlinedButton(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Google "G" logo colors
                Text(
                    text = "G",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
                Text(
                    "Continue with Google",
                    color = Color(0xFF444444),
                    fontSize = 16.sp
                )
            }
        }

        // Continue with Apple
        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VahiAppleButtonBg
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "",
                    fontSize = 20.sp
                )
                Text(
                    "Continue with Apple",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/* ---------------- STYLED FIELD ---------------- */

@Composable
private fun VahiStyledField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isPassword: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Label above the field
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333)
        )

        // Styled input field with lavender border
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = VahiTextPlaceholder
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            visualTransformation = if (isPassword)
                PasswordVisualTransformation()
            else
                VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = VahiInputBorder,
                focusedBorderColor = VahiInputBorderFocused,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                cursorColor = VahiAccentPurple
            )
        )
    }
}

/* ---------------- COMPACT / MEDIUM (tablet-only for v1 — decision D2) ---------------- */

@Composable
private fun AuthFallbackLayout() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Vahi works best on a tablet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Please open Vahi on a tablet-sized screen to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}
