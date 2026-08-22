package com.school.studentid.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
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
import com.school.studentid.PrintHelper
import com.school.studentid.Student
import com.school.studentid.StudentViewModel
import com.school.studentid.ui.components.AppButton
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentListScreen(
    viewModel: StudentViewModel,
    classFolder: String,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onStudentClick: (Student) -> Unit,
    onViewDetailsClick: (Student) -> Unit
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
        bottomBar = {
            AppButton(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.school.studentid.ui.theme.AppButtonColor,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier.fillMaxWidth().padding(12.dp).height(52.dp)
            ) {
                Text("+ Add New Student", style = MaterialTheme.typography.titleMedium)
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

            AppButton(
                onClick = {
                    scope.launch {
                        isExporting = true
                        val pdfFile = viewModel.exportPdf(classFolder)
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.school.studentid.ui.theme.AppButtonColor,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export & Share PDF (whole class)")
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn {
                items(students, key = { it.id }) { student ->
                    StudentRow(
                        student = student,
                        onEdit = { onStudentClick(student) },
                        onDeleteRequest = { studentPendingDelete = student },
                        onShare = {
                            scope.launch {
                                val file = viewModel.exportSingleStudentPdf(student)
                                if (file == null) {
                                    snackbarHostState.showSnackbar("Could not generate PDF")
                                    return@launch
                                }
                                val uri = FileProvider.getUriForFile(
                                    context, "com.school.studentid.fileprovider", file
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share ID card"))
                            }
                        },
                        onPrint = {
                            scope.launch {
                                val file = viewModel.exportSingleStudentPdf(student)
                                if (file == null) {
                                    snackbarHostState.showSnackbar("Could not generate PDF")
                                    return@launch
                                }
                                PrintHelper.printPdf(context, file, "ID Card - ${student.studentName}")
                            }
                        },
                        onViewDetails = { onViewDetailsClick(student) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (isExporting) {
        AlertDialog(
            onDismissRequest = { },
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
private fun StudentRow(
    student: Student,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onViewDetails: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),
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
                val subtitle = buildString {
                    append("Roll ${student.rollNumber} · ${student.className}")
                    if (student.section.isNotBlank()) append(" - ${student.section}")
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("View Details") },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                        onClick = { menuExpanded = false; onViewDetails() }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = { menuExpanded = false; onShare() }
                    )
                    DropdownMenuItem(
                        text = { Text("Print ID Card") },
                        leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                        onClick = { menuExpanded = false; onPrint() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { menuExpanded = false; onDeleteRequest() }
                    )
                }
            }
        }
    }
}
