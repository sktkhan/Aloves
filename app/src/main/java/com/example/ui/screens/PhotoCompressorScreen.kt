package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.util.CompressedImageResult
import com.example.util.FileExportHelper
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TARGET_PRESETS_KB = listOf(50, 100, 200, 500)

@Composable
fun PhotoCompressorScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var targetKbSlider by remember { mutableFloatStateOf(100f) }
    var isCompressing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<CompressedImageResult?>(null) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            result = null
            savedMessage = null
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
                        text = "Photo Size Compressor",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Shrink large photos to an exact KB target for passports, job portals & WhatsApp.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Target KB Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Target File Size", style = MaterialTheme.typography.titleMedium)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${targetKbSlider.roundToInt()} KB",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TARGET_PRESETS_KB.forEach { preset ->
                                FilterChip(
                                    selected = targetKbSlider.roundToInt() == preset,
                                    onClick = { targetKbSlider = preset.toFloat() },
                                    label = { Text("${preset}KB") }
                                )
                            }
                        }

                        Slider(
                            value = targetKbSlider,
                            onValueChange = { targetKbSlider = it },
                            valueRange = 25f..1000f
                        )
                    }
                }
            }

            // Pick & Compress buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_compress_photo_button")
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (selectedUri == null) "Pick Photo" else "Change Photo")
                    }

                    Button(
                        onClick = {
                            val uri = selectedUri ?: return@Button
                            isCompressing = true
                            savedMessage = null
                            errorMessage = null
                            scope.launch {
                                runCatching {
                                    FileExportHelper.compressImageToTarget(
                                        context = context,
                                        uri = uri,
                                        targetKb = targetKbSlider.roundToInt()
                                    )
                                }.onSuccess { compressed ->
                                    isCompressing = false
                                    result = compressed
                                    AdManager.showInterstitial(activity)
                                }.onFailure { err ->
                                    isCompressing = false
                                    errorMessage = err.localizedMessage ?: "Compression failed"
                                }
                            }
                        },
                        enabled = selectedUri != null && !isCompressing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_compress_button")
                    ) {
                        if (isCompressing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Working...")
                        } else {
                            Icon(Icons.Rounded.Compress, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Compress")
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

            // Result Preview or Selected Preview / Empty state
            if (result != null) {
                val res = result!!
                val savedPct = if (res.originalBytes > 0L) {
                    (((res.originalBytes - res.compressedBytes).toDouble() / res.originalBytes) * 100.0)
                        .roundToInt()
                        .coerceIn(0, 99)
                } else 0

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("compress_result_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Compression Result", style = MaterialTheme.typography.titleLarge)
                                Surface(
                                    color = EmeraldAccent.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "-$savedPct% Saved",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EmeraldAccent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatPill(
                                    label = "Before",
                                    value = FileExportHelper.formatBytes(res.originalBytes),
                                    modifier = Modifier.weight(1f)
                                )
                                StatPill(
                                    label = "After",
                                    value = FileExportHelper.formatBytes(res.compressedBytes),
                                    highlight = true,
                                    modifier = Modifier.weight(1f)
                                )
                                StatPill(
                                    label = "Dimensions",
                                    value = "${res.width}x${res.height}",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Image(
                                bitmap = res.bitmap.asImageBitmap(),
                                contentDescription = "Compressed photo preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val ok = FileExportHelper.saveImageFileToGallery(
                                            context,
                                            res.file,
                                            res.file.name
                                        )
                                        savedMessage = if (ok) {
                                            "Saved to Pictures/NexaKit!"
                                        } else {
                                            "Could not save to gallery."
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Save Gallery")
                                }
                                Button(
                                    onClick = {
                                        FileExportHelper.shareFile(
                                            context,
                                            res.file,
                                            "image/jpeg",
                                            "Share Compressed Photo"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Share")
                                }
                            }

                            if (savedMessage != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.DownloadDone,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = savedMessage.orEmpty(),
                                        color = EmeraldAccent,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (selectedUri != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val origBytes = remember(selectedUri) {
                                FileExportHelper.queryDisplaySizeBytes(context, selectedUri!!)
                            }
                            Text(
                                text = "Selected Photo • ${FileExportHelper.formatBytes(origBytes)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            AsyncImage(
                                model = selectedUri,
                                contentDescription = "Selected photo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }
                    }
                }
            } else {
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_pdf_studio),
                                contentDescription = "Photo compressor illustration",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Text(
                                text = "Pick any photo to compress cleanly",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Ideal for government forms, university uploads, and saving phone storage.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (highlight) EmeraldAccent.copy(alpha = 0.14f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = if (highlight) EmeraldAccent else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
