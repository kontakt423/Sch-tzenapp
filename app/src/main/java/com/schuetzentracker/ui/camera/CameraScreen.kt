package com.schuetzentracker.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onPhotoTaken: (Uri) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State für die Kamera-Instanz
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isTakingPhoto by remember { mutableStateOf(false) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    if (!hasPermission) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.CameraAlt, null, Modifier.size(48.dp), tint = Color.White)
                Text("Kamera-Berechtigung erforderlich",
                    color = Color.White, fontWeight = FontWeight.Bold)
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Berechtigung erteilen")
                }
                TextButton(onClick = onClose) { Text("Abbrechen", color = Color.White) }
            }
        }
        return
    }

    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    // ── FIX: Kamera nur neu binden wenn sich cameraSelector oder flashMode ändert ──
    // NICHT im AndroidView.update-Block! Das würde bei jeder Recomposition
    // die Kamera schließen und neu öffnen → "camera is closed" Fehler.
    val cameraSelector = if (useFrontCamera)
        CameraSelector.DEFAULT_FRONT_CAMERA
    else
        CameraSelector.DEFAULT_BACK_CAMERA

    LaunchedEffect(previewView, cameraSelector, flashMode) {
        val pv = previewView ?: return@LaunchedEffect
        imageCapture = bindCamera(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = pv,
            cameraSelector = cameraSelector,
            flashMode = flashMode
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Kamera-Vorschau (nur factory, kein update) ──
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { pv ->
                    previewView = pv   // State setzen → LaunchedEffect startet
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Ausrichtungshilfen ──
        if (showGuide) TargetAlignmentGuide()

        // ── Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color.Black.copy(0.5f), CircleShape)
                    .size(44.dp)
            ) {
                Icon(Icons.Default.Close, "Schließen", tint = Color.White)
            }

            FilledTonalIconButton(
                onClick = { showGuide = !showGuide },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    if (showGuide) Icons.Default.GridOff else Icons.Default.GridOn,
                    contentDescription = "Hilfslinien",
                    tint = Color.White
                )
            }
        }

        // ── Tipp-Banner ──
        if (showGuide) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp),
                color = Color.Black.copy(0.7f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "📷 Scheibe mittig • Gute Beleuchtung • Ruhig halten",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        // ── Fehlermeldung ──
        errorMessage?.let { msg ->
            Card(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(msg, color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { errorMessage = null }) {
                        Text("OK", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // ── Bottom Controls ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(0.65f))
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Flash-Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ImageCapture.FLASH_MODE_OFF to "Aus",
                    ImageCapture.FLASH_MODE_AUTO to "Auto",
                    ImageCapture.FLASH_MODE_ON to "An"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = flashMode == mode,
                        onClick = { flashMode = mode },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                if (mode == ImageCapture.FLASH_MODE_OFF)
                                    Icons.Default.FlashOff else Icons.Default.FlashOn,
                                null, Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.White.copy(0.3f),
                            labelColor = Color.White,
                            selectedLabelColor = Color.White,
                            iconColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            // Auslöser-Reihe
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kamera wechseln
                IconButton(
                    onClick = { useFrontCamera = !useFrontCamera },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, "Kamera wechseln", tint = Color.White)
                }

                // Auslöser
                ShutterButton(
                    isCapturing = isTakingPhoto,
                    onClick = {
                        val capture = imageCapture
                        if (capture == null) {
                            errorMessage = "Kamera wird noch initialisiert. Bitte kurz warten."
                            return@ShutterButton
                        }
                        isTakingPhoto = true
                        takePhoto(
                            context = context,
                            imageCapture = capture,
                            executor = executor,
                            onSuccess = { uri ->
                                isTakingPhoto = false
                                onPhotoTaken(uri)
                            },
                            onError = { err ->
                                isTakingPhoto = false
                                errorMessage = "Fehler: $err"
                            }
                        )
                    }
                )

                Spacer(Modifier.size(52.dp))
            }

            Text(
                "Foto der Zielscheibe aufnehmen",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(0.7f),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// ────────────────────────────────────────────────
// AUSLÖSER-KNOPF
// ────────────────────────────────────────────────

@Composable
fun ShutterButton(isCapturing: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isCapturing) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "shutter_scale"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(80.dp).graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Box(modifier = Modifier.size(80.dp).border(3.dp, Color.White, CircleShape))
        IconButton(
            onClick = { if (!isCapturing) onClick() },
            modifier = Modifier.size(66.dp).clip(CircleShape)
                .background(if (isCapturing) Color.White.copy(0.7f) else Color.White)
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp
                )
            }
        }
    }
}

// ────────────────────────────────────────────────
// AUSRICHTUNGSHILFEN
// ────────────────────────────────────────────────

@Composable
fun TargetAlignmentGuide() {
    val infiniteTransition = rememberInfiniteTransition(label = "guide")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "guide_alpha"
    )
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(240.dp).border(2.dp, Color.White.copy(alpha), CircleShape))
        Box(modifier = Modifier.size(80.dp).border(1.5.dp, Color.Yellow.copy(alpha), CircleShape))
        Box(modifier = Modifier.width(1.5.dp).height(240.dp).background(Color.White.copy(0.3f)))
        Box(modifier = Modifier.width(240.dp).height(1.5.dp).background(Color.White.copy(0.3f)))
    }
}

// ────────────────────────────────────────────────
// KAMERA SETUP – gibt ImageCapture zurück
// ────────────────────────────────────────────────

private fun bindCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    cameraSelector: CameraSelector,
    flashMode: Int
): ImageCapture? {
    return try {
        val future = ProcessCameraProvider.getInstance(context)
        // Synchron warten (läuft im LaunchedEffect-Coroutine-Kontext)
        val provider = future.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
            .build()

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, capture)
        capture
    } catch (e: Exception) {
        Log.e("CameraX", "Kamera-Bindung fehlgeschlagen: ${e.message}")
        null
    }
}

// ────────────────────────────────────────────────
// FOTO AUFNEHMEN
// ────────────────────────────────────────────────

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: java.util.concurrent.ExecutorService,
    onSuccess: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val file = File(
        context.cacheDir,
        "scheibe_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMANY).format(Date())}.jpg"
    )

    imageCapture.takePicture(
        ImageCapture.OutputFileOptions.Builder(file).build(),
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                onSuccess(Uri.fromFile(file))
            }
            override fun onError(exception: ImageCaptureException) {
                onError(exception.message ?: "Unbekannter Fehler")
            }
        }
    )
}
