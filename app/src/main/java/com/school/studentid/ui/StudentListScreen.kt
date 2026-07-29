package com.school.studentid.ui

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.school.studentid.Student
import com.school.studentid.StudentViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    viewModel: StudentViewModel,
    onAddClick: () -> Unit,
    onStudentClick: (Student) -> Unit
) {
    val students by viewModel.students.collectAsState()
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Holds the student awaiting delete confirmation (null = no dialog shown)
    var studentPendingDelete by remember { mutableStateOf<Student?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Students (${students.size})") })
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
                label = { Text("Search by name, roll no, or class") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        val files = viewModel.exportForSharing()
                        if (files.isEmpty()) {
                            snackbarHostState.showSnackbar("No students to export yet")
                            return@launch
                        }

                        val uris = files.map { f ->
                            FileProvider.getUriForFile(context, "com.school.studentid.fileprovider", f)
                        }

                        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "*/*"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        // Explicitly grant read permission on every attached file
                        // (needed for multiple attachments to work reliably).
                        val clip = ClipData.newUri(context.contentResolver, "Student data", uris.first())
                        for (i in 1 until uris.size) {
                            clip.addItem(ClipData.Item(uris[i]))
                        }
                        intent.clipData = clip

                        context.startActivity(Intent.createChooser(intent, "Share student data & photos"))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export & Share (data + photos)")
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn {
                items(students, key = { it.id }) { student ->
                    StudentRow(
                        student = student,
                        onClick = { onStudentClick(student) },
                        onDeleteRequest = { studentPendingDelete = student }
                    )
                    Divider()
                }
            }
        }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).clickable { onClick() }) {
            Text(student.studentName, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Roll ${student.rollNumber} · Class ${student.className}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        IconButton(onClick = onDeleteRequest) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}
