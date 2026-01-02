package com.vahi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vahi.app.ui.components.WindowClass
import com.vahi.app.ui.components.windowClass

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
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            AuthCard(onLoginSuccess)
        }

        /* RIGHT — BRAND */
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Vahi", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Brand / Animation Area",
                    style = MaterialTheme.typography.bodySmall
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

Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.background,
    shadowElevation = 1.dp,
    modifier = Modifier.width(320.dp)
)
 {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Login",
                style = MaterialTheme.typography.titleLarge
            )

            VahiField(username, { username = it }, "Username")
            VahiField(email, { email = it }, "E-mail")
            VahiField(password, { password = it }, "Password", true)
            VahiField(confirmPassword, { confirmPassword = it }, "Password confirmation", true)

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onLoginSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Login")
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            OutlinedButton(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Continue with Google")
            }

            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground
                )
            ) {
                Text(
                    "Continue with Apple",
                    color = MaterialTheme.colorScheme.background
                )
            }
        }
    }
}

/* ---------------- FIELD ---------------- */

@Composable
private fun VahiField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (isPassword)
            PasswordVisualTransformation()
        else
            VisualTransformation.None
    )
}

/* ---------------- MOBILE (LATER) ---------------- */

@Composable
private fun AuthFallbackLayout() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Auth (Mobile)")
    }
}
