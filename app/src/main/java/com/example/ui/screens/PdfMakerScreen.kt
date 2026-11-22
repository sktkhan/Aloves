package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ads.AdManager
import com.example.ads.findActivity
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RoseAccent
import com.example.util.FileExportHelper
import com.example.util.PdfExportResult
import com.example.util.PdfPagePreset
import kotlinx.coroutines.launch

@Composable
fun PdfMakerScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()

    val selectedUris = remember { mutableStateListOf<Uri>() }
    var selectedPreset by remember { mutableStateOf(PdfPagePreset.A4) }
    var isGenerating by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<PdfExportResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 25)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris.addAll(uris)
            exportResult = null
            errorMessage = null
        }
    }

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
                        text = "Photo to PDF Studio",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Pick photos with zero storage permissions, arrange pages, and export a share-ready PDF.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Page size preset selector
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Page Format", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PdfPagePreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = selectedPreset == preset,
                                    onClick = { selectedPreset = preset },
                                    label = { Text(preset.label) }
                                )
                            }
                        }
                    }
                }
            }

            // Action row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_pdf_photos_button")
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (selectedUris.isEmpty()) "Select Photos" else "Add More (${selectedUris.size})")
                    }

                    Button(
                        onClick = {
                            if (selectedUris.isEmpty() || isGenerating) return@Button
                            isGenerating = true
                            errorMessage = null
                            scope.launch {
                                runCatching {
                                    FileExportHelper.createPdfFromImages(
                                        context = context,
                                        uris = selectedUris.toList(),
                                        preset = selectedPreset
                                    )
                                }.onSuccess { result ->
                                    isGenerating = false
                                    exportResult = result
                                    AdManager.showInterstitial(activity)
                                }.onFailure { err ->
                                    isGenerating = false
                                    errorMessage = err.localizedMessage ?: "Failed to build PDF"
                                }
                            }
                        },
                        enabled = selectedUris.isNotEmpty() && !isGenerating,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_pdf_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Building...")
                        } else {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create PDF")
                        }
                    }
                }
            }

            // Export Success Card
            if (exportResult != null) {
                val res = exportResult!!
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = EmeraldAccent.copy(alpha = 0.12f)
                        ),
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.testTag("pdf_success_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldAccent)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "PDF Ready (${res.pageCount} page${if (res.pageCount == 1) "" else "s"})",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${res.displayName} • ${FileExportHelper.formatBytes(res.sizeBytes)}" +
                                            if (res.savedToDownloads) " • Saved to Downloads/NexaKit" else "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { FileExportHelper.openFile(context, res.file, "application/pdf") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Open PDF")
                                }
                                Button(
                                    onClick = {
                                        FileExportHelper.shareFile(
                                            context,
                                            res.file,
                                            "application/pdf",
                                            "Share PDF Document"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Share")
                                }
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = RoseAccent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Empty State or Selected Page List
            if (selectedUris.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_pdf_studio),
                                contentDescription = "PDF Studio illustration",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Text(
                                text = "Select photos to build a multi-page PDF",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Perfect for ID cards, bills, homework notes, and office scans. Works 100% offline.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(selectedUris, key = { idx, uri -> "$idx-$uri" }) { index, uri ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Page ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Page ${index + 1}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    val sizeBytes = remember(uri) {
                                        FileExportHelper.queryDisplaySizeBytes(context, uri)
                                    }
                                    Text(
                                        text = if (sizeBytes > 0) FileExportHelper.formatBytes(sizeBytes) else "Image selected",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFamily
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    enabled = index > 0,
                                    onClick = {
                                        val item = selectedUris.removeAt(index)
                                        selectedUris.add(index - 1, item)
                                    }
                                ) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up")
                                }
                                IconButton(
                                    enabled = index < selectedUris.lastIndex,
                                    onClick = {
                                        val item = selectedUris.removeAt(index)
                                        selectedUris.add(index + 1, item)
                                    }
                                ) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down")
                                }
                                IconButton(
                                    onClick = { selectedUris.removeAt(index) }
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = RoseAccent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
