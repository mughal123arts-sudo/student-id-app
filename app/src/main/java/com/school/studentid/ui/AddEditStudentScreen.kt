package com.school.studentid.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
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
import com.school.studentid.ImageStorage
import com.school.studentid.Student
import com.school.studentid.StudentViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStudentScreen(
    viewModel: StudentViewModel,
    existingStudent: Student?,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    // Keying each remember on existingStudent means that when the real
    // student record arrives (it starts out null while navigating, then
    // gets loaded a moment later), these fields reset to its actual values
    // instead of staying stuck on the empty "new student" defaults. This
    // was the reason Edit/View screens looked blank before.
    var studentName by remember(existingStudent) { mutableStateOf(existingStudent?.studentName ?: "") }
    var fatherName by remember(existingStudent) { mutableStateOf(existingStudent?.fatherName ?: "") }
    var className by remember(existingStudent) { mutableStateOf(existingStudent?.className ?: "") }
    var rollNumber by remember(existingStudent) { mutableStateOf(existingStudent?.rollNumber ?: "") }
    var mobileNumber by remember(existingStudent) { mutableStateOf(existingStudent?.mobileNumber ?: "") }
    var photoUri by remember(existingStudent) { mutableStateOf(existingStudent?.photoUri) }

    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Copy the picked image into the app's own storage right away so
            // it keeps working even after the app is closed and reopened.
            val savedPath = ImageStorage.copyToAppStorage(context, uri)
            if (savedPath != null) photoUri = savedPath
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraFile != null) {
            photoUri = pendingCameraFile!!.absolutePath
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = ImageStorage.newCameraOutputFile(context)
            pendingCameraFile = file
            val uri = FileProvider.getUriForFile(context, "com.school.studentid.fileprovider", file)
            cameraLauncher.launch(uri)
        }
    }

    var showPhotoOptions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (existingStudent == null) "Add Student" else "Edit Student") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ---- Photo (camera or gallery) ----
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .clickable { showPhotoOptions = true },
                contentAlignment = Alignment.Center
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = File(photoUri!!),
                        contentDescription = "Student photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(110.dp).clip(CircleShape)
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp))
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedButton(onClick = { showPhotoOptions = true }) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Take Photo / Choose from Gallery")
            }

            if (showPhotoOptions) {
                AlertDialog(
                    onDismissRequest = { showPhotoOptions = false },
                    title = { Text("Student Photo") },
                    text = { Text("Take a new photo or choose one from the gallery.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showPhotoOptions = false
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Camera")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showPhotoOptions = false
                            galleryPicker.launch("image/*")
                        }) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Gallery")
                        }
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // All fields below are optional now — no "required" validation.

            OutlinedTextField(
                value = studentName,
                onValueChange = { studentName = it },
                label = { Text("Student Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = fatherName,
                onValueChange = { fatherName = it },
                label = { Text("Father Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = className,
                onValueChange = { className = it },
                label = { Text("Class") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = rollNumber,
                onValueChange = { rollNumber = it },
                label = { Text("Roll Number / ID Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { mobileNumber = it },
                label = { Text("Contact / Mobile Number") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val student = Student(
                        id = existingStudent?.id ?: 0,
                        studentName = studentName.trim(),
                        fatherName = fatherName.trim(),
                        className = className.trim(),
                        rollNumber = rollNumber.trim(),
                        mobileNumber = mobileNumber.trim(),
                        photoUri = photoUri
                    )

                    if (existingStudent == null) {
                        viewModel.addStudent(student) { onDone() }
                    } else {
                        viewModel.updateStudent(student) { onDone() }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("SAVE", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
