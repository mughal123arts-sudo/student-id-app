package com.school.studentid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.school.studentid.ui.components.AppButton

/**
 * Basic admin login gate before the class folders / student list is shown.
 *
 * NOTE: credentials are checked locally on-device (no server). Default
 * login is admin / admin123 — change ADMIN_USERNAME / ADMIN_PASSWORD
 * below to whatever your school wants, or wire this up to a real backend
 * later if multiple admins with separate accounts are needed.
 */
private const val ADMIN_USERNAME = "admin"
private const val ADMIN_PASSWORD = "admin123"

@Composable
fun AdminLoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf(ADMIN_USERNAME) }
    var password by remember { mutableStateOf(ADMIN_PASSWORD) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.school.studentid.R.drawable.splash_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(0.7f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "School Admin Login",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; error = null },
                    label = { Text("Username") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(20.dp))

                AppButton(
                    onClick = {
                        if (username.trim() == ADMIN_USERNAME && password == ADMIN_PASSWORD) {
                            onLoginSuccess()
                        } else {
                            error = "Incorrect username or password"
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.school.studentid.ui.theme.AppButtonColor,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Login")
                }
            }
        }
    }
}
