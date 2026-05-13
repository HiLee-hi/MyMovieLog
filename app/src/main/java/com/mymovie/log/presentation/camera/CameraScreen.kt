package com.mymovie.log.presentation.camera

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File

@Composable
fun CameraScreen(
    onPhotoTaken: (Uri) -> Unit,
    onBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var cameraPermissionGranted by remember {
        mutableStateOf(
            PermissionChecker.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PermissionChecker.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (!granted) onBack()
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(uiState) {
        (uiState as? CameraUiState.SavedToGallery)?.let { onPhotoTaken(it.uri) }
    }

    if (!cameraPermissionGranted) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = uiState) {
            is CameraUiState.Preview -> CameraPreviewContent(
                onPhotoCaptured = { file -> viewModel.onPhotoCaptured(file) },
                onCaptureError = { msg -> viewModel.onCaptureError(msg) },
                onBack = {
                    viewModel.onRetake()
                    onBack()
                }
            )

            is CameraUiState.Captured -> CapturedPhotoContent(
                tempFile = state.tempFile,
                onRetake = { viewModel.onRetake() },
                onUsePhoto = { viewModel.saveToGallery(state.tempFile) }
            )

            is CameraUiState.Saving -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            is CameraUiState.SavedToGallery -> Unit

            is CameraUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
                CloseButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CameraPreviewContent(
    onPhotoCaptured: (File) -> Unit,
    onCaptureError: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val previewUseCase = remember { Preview.Builder().build() }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val cameraProvider = suspendCancellableCoroutine { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                { cont.resume(future.get()) {} },
                ContextCompat.getMainExecutor(context)
            )
        }
        previewUseCase.setSurfaceProvider(previewView.surfaceProvider)
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            previewUseCase,
            imageCapture
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        CloseButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        ShutterButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            onClick = {
                val tempFile = File(
                    context.externalCacheDir,
                    "camera_temp_${System.currentTimeMillis()}.jpg"
                )
                val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            onPhotoCaptured(tempFile)
                        }
                        override fun onError(e: ImageCaptureException) {
                            tempFile.delete()
                            onCaptureError(e.message ?: "촬영 실패")
                        }
                    }
                )
            }
        )
    }
}

@Composable
private fun CapturedPhotoContent(
    tempFile: File,
    onRetake: () -> Unit,
    onUsePhoto: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = tempFile,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onRetake,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("다시찍기")
            }

            Button(onClick = onUsePhoto) {
                Text("사용하기")
            }
        }
    }
}

@Composable
private fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .background(Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "닫기",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
