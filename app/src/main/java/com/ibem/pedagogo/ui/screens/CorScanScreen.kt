package com.ibem.pedagogo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ibem.pedagogo.ui.scan.CorScanPhase
import com.ibem.pedagogo.ui.scan.CorScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorScanScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    vm: CorScanViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    // Full-res capture: TakePicture + FileProvider (the preview contract
    // used to return a ~1MP thumbnail - too small for dense COR tables).
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val newPhotoUri: () -> Uri = {
        val dir = File(context.cacheDir, "cor").apply { mkdirs() }
        val file = File(dir, "cor_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        val uri = pendingPhotoUri
        if (saved && uri != null) vm.recognizeUri(context, uri)
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) vm.recognizePdf(context, uri) }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) vm.recognizeUri(context, uri) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = newPhotoUri()
            pendingPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Scan your COR", fontWeight = FontWeight.Bold)
                        Text(
                            "Photo first, confirm in seconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (state.phase) {
            CorScanPhase.CAPTURE -> CorCaptureContent(
                modifier = Modifier.padding(padding),
                error = state.error,
                onCamera = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        val uri = newPhotoUri()
                        pendingPhotoUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPdf = { pdfLauncher.launch("application/pdf") },
                queuedPages = state.pageCount,
                onPaste = { vm.parsePastedText(it) }
            )
            CorScanPhase.SCANNING -> CorStatusContent(Modifier.padding(padding), "Reading your COR…")
            CorScanPhase.CONFIRM -> CorConfirmContent(
                modifier = Modifier.padding(padding),
                vm = vm,
                onAddPage = { vm.addAnotherPage() },
                onRetake = { vm.reset() }
            )
            CorScanPhase.SAVING -> CorStatusContent(Modifier.padding(padding), "Saving your schedule…")
            CorScanPhase.DONE -> CorDoneContent(
                modifier = Modifier.padding(padding),
                saved = state.savedCount,
                alarmed = state.alarmedCount,
                onFinish = { vm.reset(); onDone() }
            )
        }
    }
}
