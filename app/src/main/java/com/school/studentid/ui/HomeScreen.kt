package com.school.studentid.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.school.studentid.AppPreferences
import com.school.studentid.ClassConstants
import com.school.studentid.R
import com.school.studentid.StudentViewModel
import com.school.studentid.ui.theme.AppFolderCardColor
import com.school.studentid.ui.theme.AppPrimary
import com.school.studentid.ui.theme.AppPrimaryContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: StudentViewModel,
    onFolderClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val stats by viewModel.dashboardStats.collectAsState()
    val folderCounts by viewModel.folderCounts.collectAsState()

    var viewMode by remember { mutableStateOf(AppPreferences.getViewMode(context)) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(AppPrimary, AppPrimaryContainer)))
            ) {
                CenterAlignedTopAppBar(
                    title = { Text("Home", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(36.dp)
                        )
                    },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            // ---- Dashboard summary (compact, single row) ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardCard(Icons.Default.Groups, "Students", stats.totalStudents.toString(), Modifier.weight(1f))
                DashboardCard(Icons.Default.School, "Classes", stats.totalClasses.toString(), Modifier.weight(1f))
                DashboardCard(Icons.Default.Photo, "Photos", stats.totalPhotos.toString(), Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            // ---- Prominent "Select a Class" header ----
            Text(
                "Select a Class",
                style = MaterialTheme.typography.headlineSmall,
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
                        tint = if (viewMode == "grid") AppPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    viewMode = "list"
                    AppPreferences.setViewMode(context, "list")
                }) {
                    Icon(
                        Icons.Default.ViewList,
                        contentDescription = "List view",
                        tint = if (viewMode == "list") AppPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ---- Folder section (subtle crossfade between grid/list) ----
            Crossfade(targetState = viewMode, label = "folderViewMode") { mode ->
                if (mode == "grid") {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(ClassConstants.ALL_FOLDERS) { folderName ->
                            FolderListRow(
                                folderName = folderName,
                                count = folderCounts[folderName] ?: 0,
                                onClick = { onFolderClick(folderName) }
                            )
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
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = AppPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FolderGridCard(folderName: String, count: Int, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AppFolderCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = AppPrimary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                folderName,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text("$count student${if (count == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FolderListRow(folderName: String, count: Int, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AppFolderCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = AppPrimary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Text(folderName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("$count", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
