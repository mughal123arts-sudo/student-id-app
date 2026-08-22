package com.school.studentid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.school.studentid.AppPreferences
import com.school.studentid.ui.components.AppButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var schoolName by remember { mutableStateOf(AppPreferences.getSchoolName(context)) }
    var schoolAddress by remember { mutableStateOf(AppPreferences.getSchoolAddress(context)) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp)) {
            Text("School Information", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it; saved = false },
                label = { Text("School Name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = schoolAddress,
                onValueChange = { schoolAddress = it; saved = false },
                label = { Text("School Address") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            AppButton(
                onClick = {
                    AppPreferences.saveSchoolInfo(context, schoolName.trim(), schoolAddress.trim())
                    saved = true
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.school.studentid.ui.theme.AppButtonColor,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Save Changes")
            }

            if (saved) {
                Spacer(Modifier.height(12.dp))
                Text("Saved!", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
