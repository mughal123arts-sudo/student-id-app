package com.school.studentid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.school.studentid.AppPreferences
import com.school.studentid.ui.components.AppButton

@Composable
fun SetupScreen(onSetupDone: () -> Unit) {
    val context = LocalContext.current
    var schoolName by remember { mutableStateOf("") }
    var schoolAddress by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Welcome! Let's set up your school",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "This is only needed once — you'll just log in after this.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = schoolName,
                    onValueChange = { schoolName = it },
                    label = { Text("School Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = schoolAddress,
                    onValueChange = { schoolAddress = it },
                    label = { Text("School Address") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))

                AppButton(
                    onClick = {
                        AppPreferences.saveSchoolInfo(context, schoolName.trim(), schoolAddress.trim())
                        onSetupDone()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.school.studentid.ui.theme.AppButtonColor,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Continue")
                }
            }
        }
    }
}
