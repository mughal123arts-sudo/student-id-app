package com.school.studentid.ui

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.school.studentid.ImageStorage
import com.school.studentid.Student
import com.school.studentid.StudentViewModel
import com.school.studentid.ui.components.AppButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStudentScreen(
    viewModel: StudentViewModel,
    existingStudent: Student?,
    presetClassName: String? = null,
    lockClassField: Boolean = false,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    var studentName by remember(existingStudent) { mutableStateOf(existingStudent?.studentName ?: "") }
    var fatherName by remember(existingStudent) { mutableStateOf(existingStudent?.fatherName ?: "") }
    var className by remember(existingStudent) {
        mutableStateOf(existingStudent?.className ?: presetClassName ?: "")
    }
    var section by remember(existingStudent) { mutableStateOf(existingStudent?.section ?: "") }
    var rollNumber by remember(existingStudent) { mutableStateOf(existingStudent?.rollNumber ?: "") }
    var mobileNumber by remember(existingStudent) { mutableStateOf(existingStudent?.mobileNumber ?: "") }
    var notes by remember(existingStudent) { mutableStateOf(existingStudent?.notes ?: "") }
    var photoUri by remember(existingStudent) { mutableStateOf(existingStudent?.photoUri) }

    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val savedPath = ImageStorage.copyToAppStorage(context, uri)
                if (savedPath != null) {
                    withContext(Dispatchers.Main) { photoUri = savedPath }
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraFile != null) {
            val capturedFile = pendingCameraFile!!
            scope.launch(Dispatchers.IO) {
                val optimizedPath = ImageStorage.optimizeCapturedPhoto(context, capturedFile)
                if (optimizedPath != null) {
                    withContext(Dispatchers.Main) { photoUri = optimizedPath }
                }
            }
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

    // Pick a phone number directly from the phone's Contacts app.
    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri = result.data?.data
            if (contactUri != null) {
                context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numberIndex >= 0) {
                            mobileNumber = cursor.getString(numberIndex) ?: mobileNumber
                        }
                    }
                }
            }
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

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showPhotoOptions = true },
                contentAlignment = Alignment.Center
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = File(photoUri!!),
                        contentDescription = "Student photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(56.dp))
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedButton(onClick = { showPhotoOptions = true }, shape = RoundedCornerShape(12.dp)) {
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

            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("Student Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = fatherName,
                        onValueChange = { fatherName = it },
                        label = { Text("Father Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = className,
                        onValueChange = { if (!lockClassField) className = it },
                        label = { Text(if (lockClassField) "Class (locked)" else "Class") },
                        singleLine = true,
                        readOnly = lockClassField,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Section (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = rollNumber,
                        onValueChange = { rollNumber = it },
                        label = { Text("Roll Number / ID Number") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = { mobileNumber = it },
                        label = { Text("Contact / Mobile Number") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        ),
                        trailingIcon = {
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                contactPickerLauncher.launch(intent)
                            }) {
                                Icon(Icons.Default.ContactPhone, contentDescription = "Pick from contacts")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            AppButton(
                onClick = {
                    val student = Student(
                        id = existingStudent?.id ?: 0,
                        studentName = studentName.trim(),
                        fatherName = fatherName.trim(),
                        className = className.trim(),
                        section = section.trim(),
                        rollNumber = rollNumber.trim(),
                        mobileNumber = mobileNumber.trim(),
                        photoUri = photoUri,
                        notes = notes.trim()
                    )

                    if (existingStudent == null) {
                        viewModel.addStudent(student) { onDone() }
                    } else {
                        viewModel.updateStudent(student) { onDone() }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.school.studentid.ui.theme.AppButtonColor,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("SAVE", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
