package com.school.studentid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.school.studentid.AppPreferences
import com.school.studentid.ClassConstants
import com.school.studentid.Student
import com.school.studentid.StudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: StudentViewModel,
    onFolderClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onStudentClick: (Student) -> Unit
) {
    val context = LocalContext.current
    val stats by viewModel.dashboardStats.collectAsState()
    val folderCounts by viewModel.folderCounts.collectAsState()
    val globalResults by viewModel.globalSearchResults.collectAsState()

    var globalQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(AppPreferences.getViewMode(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            // ---- Dashboard summary ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardCard(Icons.Default.Groups, "Students", stats.totalStudents.toString(), Modifier.weight(1f))
                DashboardCard(Icons.Default.School, "Classes", stats.totalClasses.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardCard(Icons.Default.Photo, "Photos", stats.totalPhotos.toString(), Modifier.weight(1f))
                DashboardCard(Icons.Default.Save, "Storage", formatBytes(stats.storageUsedBytes), Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            // ---- Global search ----
            OutlinedTextField(
                value = globalQuery,
                onValueChange = {
                    globalQuery = it
                    viewModel.setGlobalSearchQuery(it)
                },
                label = { Text("Search students across all classes") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (globalQuery.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(globalResults, key = { it.id }) { student ->
                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onStudentClick(student) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(student.studentName, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Roll ${student.rollNumber} · ${student.className}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    if (globalResults.isEmpty()) {
                        item {
                            Text(
                                "No students found",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(24.dp))

                // ---- Prominent "Select a Class" header ----
                Text(
                    "Select a Class",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // ---- Grid / List toggle ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = {
                        viewMode = "grid"
                        AppPreferences.setViewMode(context, "grid")
                    }) {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = "Grid view",
                            tint = if (viewMode == "grid") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        viewMode = "list"
                        AppPreferences.setViewMode(context, "list")
                    }) {
                        Icon(
                            Icons.Default.ViewList,
                            contentDescription = "List view",
                            tint = if (viewMode == "list") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                if (viewMode == "grid") {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        gridItems(ClassConstants.ALL_FOLDERS) { folderName ->
                            FolderGridCard(
                                folderName = folderName,
                                count = folderCounts[folderName] ?: 0,
                                onClick = { onFolderClick(folderName) }
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(ClassConstants.ALL_FOLDERS) { folderName ->
                            FolderListRow(
                                folderName = folderName,
                                count = folderCounts[folderName] ?: 0,
                                onClick = { onFolderClick(folderName) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FolderGridCard(folderName: String, count: Int, onClick: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(folderName, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Text("$count student${if (count == 1) "" else "s"}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FolderListRow(folderName: String, count: Int, onClick: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(folderName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text("$count", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}
