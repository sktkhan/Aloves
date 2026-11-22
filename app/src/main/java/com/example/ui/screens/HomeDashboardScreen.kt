package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ads.AdManager
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.VioletAccent

enum class NexaToolRoute(val route: String) {
    HOME("home"),
    PDF_MAKER("pdf_maker"),
    COMPRESSOR("compressor"),
    QR_STUDIO("qr_studio"),
    FINANCE("finance"),
    CONVERTER("converter"),
    HEALTH_SENSORS("health_sensors")
}

private data class ToolTile(
    val route: NexaToolRoute,
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector,
    val accent: Color,
    val keywords: String,
    val testTag: String
)

private val ALL_TOOLS = listOf(
    ToolTile(
        route = NexaToolRoute.PDF_MAKER,
        title = "Photo to PDF",
        subtitle = "Merge receipts, IDs & notes into crisp A4 PDFs",
        badge = "MOST USED",
        icon = Icons.Rounded.PictureAsPdf,
        accent = RoseAccent,
        keywords = "pdf document scan photo merge export print",
        testTag = "tool_card_pdf"
    ),
    ToolTile(
        route = NexaToolRoute.COMPRESSOR,
        title = "Photo Compressor",
        subtitle = "Shrink photos to 50KB / 100KB for online forms",
        badge = "KB TARGET",
        icon = Icons.Rounded.Compress,
        accent = EmeraldAccent,
        keywords = "compress resize kb mb photo image size reduce",
        testTag = "tool_card_compressor"
    ),
    ToolTile(
        route = NexaToolRoute.QR_STUDIO,
        title = "QR Studio",
        subtitle = "Live camera scanner + Wi-Fi & link QR maker",
        badge = "SCAN & MAKE",
        icon = Icons.Rounded.QrCodeScanner,
        accent = CobaltAccent,
        keywords = "qr barcode scanner wifi generator link whatsapp",
        testTag = "tool_card_qr"
    ),
    ToolTile(
        route = NexaToolRoute.FINANCE,
        title = "Finance & EMI",
        subtitle = "Loan EMI chart, GST/Discount & split bill math",
        badge = "SMART MATH",
        icon = Icons.Rounded.Calculate,
        accent = AmberAccent,
        keywords = "emi loan interest discount gst tax tip bill split",
        testTag = "tool_card_finance"
    ),
    ToolTile(
        route = NexaToolRoute.CONVERTER,
        title = "Unit Converter",
        subtitle = "Length, weight, temp, area, data & speed",
        badge = "8 CATEGORIES",
        icon = Icons.Rounded.SwapHoriz,
        accent = CyanAccent,
        keywords = "unit convert meter kg km celsius data mb gb",
        testTag = "tool_card_converter"
    ),
    ToolTile(
        route = NexaToolRoute.HEALTH_SENSORS,
        title = "Health & Compass",
        subtitle = "BMI gauge, exact age & live sensor compass",
        badge = "PRO SENSORS",
        icon = Icons.Rounded.Explore,
        accent = VioletAccent,
        keywords = "bmi calories age birthday compass bubble level sensor",
        testTag = "tool_card_health"
    )
)

@Composable
fun HomeDashboardScreen(
    onNavigateTool: (NexaToolRoute) -> Unit,
    onOpenAdConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val adState by AdManager.state.collectAsStateWithLifecycle()

    val filteredTools = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) ALL_TOOLS
        else ALL_TOOLS.filter {
            it.title.lowercase().contains(q) ||
                it.subtitle.lowercase().contains(q) ||
                it.keywords.contains(q)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(174.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_toolkit),
                            contentDescription = "NexaKit toolkit banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF0A1224).copy(alpha = 0.94f),
                                            Color(0xFF0A1224).copy(alpha = 0.72f),
                                            Color(0xFF0A1224).copy(alpha = 0.28f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = EmeraldAccent.copy(alpha = 0.22f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "ALL-IN-ONE UTILITY SUITE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA7F3D0),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    onClick = onOpenAdConsole,
                                    color = if (adState.isAdFreeActive) {
                                        EmeraldAccent.copy(alpha = 0.25f)
                                    } else {
                                        Color.White.copy(alpha = 0.14f)
                                    },
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                                    modifier = Modifier.testTag("hero_ad_console_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (adState.isAdFreeActive) {
                                                Icons.Rounded.Verified
                                            } else {
                                                Icons.Rounded.AdsClick
                                            },
                                            contentDescription = "AdMob Status",
                                            tint = if (adState.isAdFreeActive) Color(0xFF6EE7B7) else Color(0xFFFDE68A),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = if (adState.isAdFreeActive) {
                                                "Pro ${adState.adFreeRemainingMinutes}m"
                                            } else {
                                                "AdMob Live"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Solve Everyday Tasks\nin Seconds",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "PDFs, KB photo shrinking, QR scanning, EMI & live sensors — zero clutter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Search bar across tools
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Search tools (PDF, 50KB, WiFi QR, EMI, BMI, Compass)...")
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_input")
                )
            }

            // Quick Bento Grid (2 columns)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Essential Problem Solvers",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${filteredTools.size} tools",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (filteredTools.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "No matching tool found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Try keywords like PDF, compress, QR, EMI, unit, or compass.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val rows = filteredTools.chunked(2)
                items(rows.size) { rowIndex ->
                    val pair = rows[rowIndex]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { tool ->
                            ToolGridCard(
                                tool = tool,
                                onClick = { onNavigateTool(tool.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // Rewarded Ad / Pro Pass Promo Strip
            item {
                Card(
                    onClick = onOpenAdConsole,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (adState.isAdFreeActive) EmeraldAccent.copy(alpha = 0.55f)
                        else AmberAccent.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_reward_banner_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = if (adState.isAdFreeActive) {
                                    EmeraldAccent.copy(alpha = 0.14f)
                                } else {
                                    AmberAccent.copy(alpha = 0.14f)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.WorkspacePremium,
                                        contentDescription = null,
                                        tint = if (adState.isAdFreeActive) EmeraldAccent else AmberAccent
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (adState.isAdFreeActive) {
                                        "Ad-Free Pass Active (${adState.adFreeRemainingMinutes}m left)"
                                    } else {
                                        "AdMob Hub & 2h Ad-Free Reward"
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (adState.isAdFreeActive) {
                                        "Tap to inspect live AdMob status or resume banners"
                                    } else {
                                        "Watch 1 Rewarded Ad for 2h ad-free, or test live AdMob ads"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Open AdMob Hub",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolGridCard(
    tool: ToolTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(tool.testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = tool.accent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = tool.title,
                            tint = tool.accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Surface(
                    color = tool.accent.copy(alpha = 0.10f),
                    shape = CircleShape
                ) {
                    Text(
                        text = tool.badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = tool.accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tool.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
