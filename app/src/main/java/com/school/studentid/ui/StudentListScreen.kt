package com.school.studentid.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.school.studentid.Student
import com.school.studentid.StudentViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    viewModel: StudentViewModel,
    classFolder: String,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onStudentClick: (Student) -> Unit
) {
    LaunchedEffect(classFolder) {
        viewModel.setClassFilter(classFolder)
    }

    val students by viewModel.students.collectAsState()
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var studentPendingDelete by remember { mutableStateOf<Student?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classFolder (${students.size})") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add student")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(12.dp)) {

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.setSearchQuery(it)
                },
                label = { Text("Search by name, father name, or roll no") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        isExporting = true
                        val pdfFile = viewModel.exportPdf()
                        isExporting = false

                        if (pdfFile == null) {
                            snackbarHostState.showSnackbar("No students to export yet")
                            return@launch
                        }

                        snackbarHostState.showSnackbar("Export successful")

                        val uri = FileProvider.getUriForFile(
                            context, "com.school.studentid.fileprovider", pdfFile
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share student PDF"))
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export & Share PDF")
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn {
                items(students, key = { it.id }) { student ->
                    StudentRow(
                        student = student,
                        onClick = { onStudentClick(student) },
                        onDeleteRequest = { studentPendingDelete = student }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // ---- Export progress dialog ----
    if (isExporting) {
        AlertDialog(
            onDismissRequest = { /* not dismissible while exporting */ },
            confirmButton = {},
            title = { Text("Please wait") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Exporting data...")
                }
            }
        )
    }

    // ---- Delete confirmation popup ----
    val toDelete = studentPendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { studentPendingDelete = null },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to delete this student?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteStudent(toDelete)
                    studentPendingDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StudentRow(student: Student, onClick: () -> Unit, onDeleteRequest: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Square thumbnail
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (student.photoUri != null) {
                    AsyncImage(
                        model = File(student.photoUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(student.studentName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Roll ${student.rollNumber} · ${student.className}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDeleteRequest) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
