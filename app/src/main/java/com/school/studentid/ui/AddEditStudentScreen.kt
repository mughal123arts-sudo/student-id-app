package com.school.studentid.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.school.studentid.ImageStorage
import com.school.studentid.Student
import com.school.studentid.StudentViewModel
import com.school.studentid.ui.components.AppButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Where a photo, awaiting a background choice, came from. */
private sealed class PendingPhoto {
    data class FromGallery(val uri: Uri) : PendingPhoto()
    data class FromCamera(val file: File) : PendingPhoto()
}

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
    var pendingPhoto by remember { mutableStateOf<PendingPhoto?>(null) }
    val scope = rememberCoroutineScope()

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) pendingPhoto = PendingPhoto.FromGallery(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraFile != null) {
            pendingPhoto = PendingPhoto.FromCamera(pendingCameraFile!!)
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

            // ---- Background picker preview, shown right after a photo is taken/picked ----
            pendingPhoto?.let { pending ->
                PhotoBackgroundPickerDialog(
                    previewModel = when (pending) {
                        is PendingPhoto.FromGallery -> pending.uri
                        is PendingPhoto.FromCamera -> pending.file
                    },
                    onCancel = { pendingPhoto = null },
                    onConfirm = { backgroundColor ->
                        scope.launch(Dispatchers.IO) {
                            val savedPath = when (pending) {
                                is PendingPhoto.FromGallery ->
                                    ImageStorage.processAndSave(context, sourceUri = pending.uri, backgroundColor = backgroundColor)
                                is PendingPhoto.FromCamera ->
                                    ImageStorage.processAndSave(context, sourceFile = pending.file, backgroundColor = backgroundColor)
                            }
                            withContext(Dispatchers.Main) {
                                if (savedPath != null) photoUri = savedPath
                                pendingPhoto = null
                            }
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

/**
 * Full-screen preview shown right after taking/picking a photo, letting the
 * admin choose a plain White or Blue backdrop, or keep the Original Image
 * as-is.
 */
@Composable
private fun PhotoBackgroundPickerDialog(
    previewModel: Any,
    onCancel: () -> Unit,
    onConfirm: (backgroundColor: Int?) -> Unit
) {
    var selected by remember { mutableStateOf("original") }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Choose Background", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                AsyncImage(
                    model = previewModel,
                    contentDescription = "Photo preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEFEFEF))
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BackgroundSwatch(
                        label = "White",
                        swatchColor = Color.White,
                        showBorder = true,
                        selected = selected == "white",
                        onClick = { selected = "white" }
                    )
                    BackgroundSwatch(
                        label = "Blue",
                        swatchColor = Color(0xFF1565C0),
                        showBorder = false,
                        selected = selected == "blue",
                        onClick = { selected = "blue" }
                    )
                    BackgroundSwatch(
                        label = "Original Image",
                        previewModel = previewModel,
                        selected = selected == "original",
                        onClick = { selected = "original" }
                    )
                }

                Spacer(Modifier.height(24.dp))

                AppButton(
                    onClick = {
                        val color = when (selected) {
                            "white" -> AndroidColor.WHITE
                            "blue" -> AndroidColor.parseColor("#1565C0")
                            else -> null
                        }
                        onConfirm(color)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.school.studentid.ui.theme.AppButtonColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Use This Photo")
                }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun BackgroundSwatch(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    swatchColor: Color? = null,
    previewModel: Any? = null,
    showBorder: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .then(
                    if (swatchColor != null) Modifier.background(swatchColor)
                    else Modifier.background(Color(0xFFEFEFEF))
                )
                .border(
                    width = if (selected) 3.dp else if (showBorder) 1.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (previewModel != null) {
                AsyncImage(
                    model = previewModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp).clip(CircleShape)
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}
