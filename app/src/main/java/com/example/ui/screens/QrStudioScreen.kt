package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.EmeraldAccent
import com.example.util.FileExportHelper
import com.example.util.QrHelper
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

private enum class QrTemplate(val label: String) {
    TEXT_URL("Link / Text"),
    WIFI("Wi-Fi"),
    WHATSAPP("WhatsApp"),
    PHONE("Phone")
}

@Composable
fun QrStudioScreen(
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Create, 1 = Scan

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "QR & Barcode Studio",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Create shareable Wi-Fi, WhatsApp & link QR codes, or scan live with Camera.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = { Icon(Icons.Rounded.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("qr_tab_create")
                    ) {
                        Text("Create QR")
                    }
                    SegmentedButton(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("qr_tab_scan")
                    ) {
                        Text("Scan Live")
                    }
                }
            }

            if (activeTab == 0) {
                item { QrGeneratorPane() }
            } else {
                item { QrScannerPane() }
            }
        }
    }
}

@Composable
private fun QrGeneratorPane() {
    val context = LocalContext.current
    var template by remember { mutableStateOf(QrTemplate.TEXT_URL) }
    var primaryField by remember { mutableStateOf("https://nexakit.app") }
    var wifiPassword by remember { mutableStateOf("") }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    val encodedPayload = remember(template, primaryField, wifiPassword) {
        val clean = primaryField.trim()
        when (template) {
            QrTemplate.TEXT_URL -> clean
            QrTemplate.WIFI -> if (clean.isEmpty()) "" else "WIFI:T:WPA;S:$clean;P:${wifiPassword.trim()};;"
            QrTemplate.WHATSAPP -> {
                val digits = clean.filter { it.isDigit() }
                if (digits.isEmpty()) "" else "https://wa.me/$digits"
            }
            QrTemplate.PHONE -> if (clean.isEmpty()) "" else "tel:$clean"
        }
    }

    val qrBitmap: Bitmap? = remember(encodedPayload) {
        if (encodedPayload.isBlank()) null else QrHelper.generateQrBitmap(encodedPayload)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QrTemplate.entries.forEach { item ->
                FilterChip(
                    selected = template == item,
                    onClick = {
                        template = item
                        statusNotice = null
                        primaryField = when (item) {
                            QrTemplate.TEXT_URL -> "https://nexakit.app"
                            QrTemplate.WIFI -> ""
                            QrTemplate.WHATSAPP -> ""
                            QrTemplate.PHONE -> ""
                        }
                    },
                    label = { Text(item.label) }
                )
            }
        }

        OutlinedTextField(
            value = primaryField,
            onValueChange = { primaryField = it; statusNotice = null },
            label = {
                Text(
                    when (template) {
                        QrTemplate.TEXT_URL -> "URL or Any Text"
                        QrTemplate.WIFI -> "Wi-Fi Network Name (SSID)"
                        QrTemplate.WHATSAPP -> "WhatsApp Number (with Country Code)"
                        QrTemplate.PHONE -> "Phone Number"
                    }
                )
            },
            singleLine = template != QrTemplate.TEXT_URL,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("qr_primary_input")
        )

        if (template == QrTemplate.WIFI) {
            OutlinedTextField(
                value = wifiPassword,
                onValueChange = { wifiPassword = it },
                label = { Text("Wi-Fi Password") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (qrBitmap != null) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 2.dp
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Generated QR code",
                            modifier = Modifier
                                .size(220.dp)
                                .padding(12.dp)
                        )
                    }
                    Text(
                        text = encodedPayload,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val ok = FileExportHelper.saveBitmapToGallery(
                                    context,
                                    qrBitmap,
                                    "NexaKit_QR_${System.currentTimeMillis()}.png"
                                )
                                statusNotice = if (ok) "QR saved to Pictures/NexaKit!" else "Save failed."
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save PNG")
                        }
                        Button(
                            onClick = {
                                val file = QrHelper.writeQrBitmapToCache(context, qrBitmap)
                                FileExportHelper.shareFile(context, file, "image/png", "Share QR Code")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share QR")
                        }
                    }
                } else {
                    Text(
                        text = "Enter details above to generate a live QR code",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (statusNotice != null) {
                    Text(
                        text = statusNotice.orEmpty(),
                        color = EmeraldAccent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun QrScannerPane() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var scannedValue by remember { mutableStateOf<String?>(null) }
    var copiedNotice by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!hasCameraPermission) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = CobaltAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                tint = CobaltAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = "Camera Access Needed to Scan",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Grant Camera permission to scan QR codes, Wi-Fi tags, and product barcodes offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.testTag("grant_camera_button")
                        ) {
                            Text("Enable Camera")
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            }
                        ) {
                            Text("App Settings")
                        }
                    }
                }
            }
        } else {
            val previewView = remember { PreviewView(context) }
            val executor = remember { Executors.newSingleThreadExecutor() }
            val scanner = remember { BarcodeScanning.getClient() }

            DisposableEffect(lifecycleOwner) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    runCatching {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { imageAnalysis ->
                                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val input = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        scanner.process(input)
                                            .addOnSuccessListener { barcodes ->
                                                val value = barcodes.firstOrNull()?.rawValue
                                                if (!value.isNullOrBlank()) {
                                                    scannedValue = value
                                                    copiedNotice = false
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    }
                }, ContextCompat.getMainExecutor(context))

                onDispose {
                    runCatching {
                        ProcessCameraProvider.getInstance(context).get().unbindAll()
                    }
                    scanner.close()
                    executor.shutdown()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .size(185.dp)
                        .border(2.5.dp, EmeraldAccent, RoundedCornerShape(18.dp))
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (scannedValue != null) EmeraldAccent.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (scannedValue != null) "Decoded Result" else "Point camera at any QR or Barcode",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (scannedValue != null) {
                        val value = scannedValue!!
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("QR Result", value))
                                    copiedNotice = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (copiedNotice) "Copied!" else "Copy")
                            }
                            if (value.startsWith("http://", ignoreCase = true) ||
                                value.startsWith("https://", ignoreCase = true)
                            ) {
                                Button(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value)))
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Open Link")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
