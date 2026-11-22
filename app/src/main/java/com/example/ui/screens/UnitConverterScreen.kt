package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale

private data class UnitDef(
    val name: String,
    val symbol: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double
)

private data class UnitCategory(
    val title: String,
    val units: List<UnitDef>
)

private val UNIT_CATEGORIES = listOf(
    UnitCategory(
        "Length",
        listOf(
            UnitDef("Meter", "m", { it }, { it }),
            UnitDef("Kilometer", "km", { it * 1000.0 }, { it / 1000.0 }),
            UnitDef("Centimeter", "cm", { it * 0.01 }, { it / 0.01 }),
            UnitDef("Millimeter", "mm", { it * 0.001 }, { it / 0.001 }),
            UnitDef("Mile", "mi", { it * 1609.344 }, { it / 1609.344 }),
            UnitDef("Yard", "yd", { it * 0.9144 }, { it / 0.9144 }),
            UnitDef("Foot", "ft", { it * 0.3048 }, { it / 0.3048 }),
            UnitDef("Inch", "in", { it * 0.0254 }, { it / 0.0254 })
        )
    ),
    UnitCategory(
        "Weight",
        listOf(
            UnitDef("Kilogram", "kg", { it }, { it }),
            UnitDef("Gram", "g", { it * 0.001 }, { it / 0.001 }),
            UnitDef("Pound", "lb", { it * 0.45359237 }, { it / 0.45359237 }),
            UnitDef("Ounce", "oz", { it * 0.0283495231 }, { it / 0.0283495231 }),
            UnitDef("Metric Ton", "t", { it * 1000.0 }, { it / 1000.0 })
        )
    ),
    UnitCategory(
        "Temp",
        listOf(
            UnitDef("Celsius", "°C", { it }, { it }),
            UnitDef("Fahrenheit", "°F", { (it - 32.0) * 5.0 / 9.0 }, { it * 9.0 / 5.0 + 32.0 }),
            UnitDef("Kelvin", "K", { it - 273.15 }, { it + 273.15 })
        )
    ),
    UnitCategory(
        "Area",
        listOf(
            UnitDef("Sq Meter", "m²", { it }, { it }),
            UnitDef("Sq Foot", "ft²", { it * 0.092903 }, { it / 0.092903 }),
            UnitDef("Acre", "ac", { it * 4046.8564 }, { it / 4046.8564 }),
            UnitDef("Hectare", "ha", { it * 10000.0 }, { it / 10000.0 }),
            UnitDef("Marla (272)", "marla", { it * 25.2929 }, { it / 25.2929 }),
            UnitDef("Kanal", "kanal", { it * 505.857 }, { it / 505.857 })
        )
    ),
    UnitCategory(
        "Volume",
        listOf(
            UnitDef("Liter", "L", { it }, { it }),
            UnitDef("Milliliter", "mL", { it * 0.001 }, { it / 0.001 }),
            UnitDef("US Gallon", "gal", { it * 3.78541 }, { it / 3.78541 }),
            UnitDef("Cup", "cup", { it * 0.236588 }, { it / 0.236588 })
        )
    ),
    UnitCategory(
        "Speed",
        listOf(
            UnitDef("km/h", "km/h", { it / 3.6 }, { it * 3.6 }),
            UnitDef("m/s", "m/s", { it }, { it }),
            UnitDef("mph", "mph", { it * 0.44704 }, { it / 0.44704 }),
            UnitDef("Knots", "kn", { it * 0.514444 }, { it / 0.514444 })
        )
    ),
    UnitCategory(
        "Data",
        listOf(
            UnitDef("Megabyte", "MB", { it }, { it }),
            UnitDef("Kilobyte", "KB", { it / 1024.0 }, { it * 1024.0 }),
            UnitDef("Gigabyte", "GB", { it * 1024.0 }, { it / 1024.0 }),
            UnitDef("Terabyte", "TB", { it * 1024.0 * 1024.0 }, { it / (1024.0 * 1024.0) })
        )
    ),
    UnitCategory(
        "Time",
        listOf(
            UnitDef("Minute", "min", { it }, { it }),
            UnitDef("Second", "s", { it / 60.0 }, { it * 60.0 }),
            UnitDef("Hour", "hr", { it * 60.0 }, { it / 60.0 }),
            UnitDef("Day", "d", { it * 1440.0 }, { it / 1440.0 }),
            UnitDef("Week", "wk", { it * 10080.0 }, { it / 10080.0 })
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(
    modifier: Modifier = Modifier
) {
    var categoryIdx by remember { mutableIntStateOf(0) }
    val category = UNIT_CATEGORIES[categoryIdx]
    var fromIdx by remember(categoryIdx) { mutableIntStateOf(0) }
    var toIdx by remember(categoryIdx) { mutableIntStateOf(if (category.units.size > 1) 1 else 0) }
    var inputText by remember { mutableStateOf("100") }

    val inputVal = inputText.toDoubleOrNull() ?: 0.0
    val fromUnit = category.units[fromIdx.coerceIn(0, category.units.lastIndex)]
    val toUnit = category.units[toIdx.coerceIn(0, category.units.lastIndex)]
    val baseVal = fromUnit.toBase(inputVal)
    val convertedVal = toUnit.fromBase(baseVal)

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
                        text = "Universal Unit Converter",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "8 everyday categories including Area (Marla/Kanal/Sq ft), Data, Temp & Speed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UNIT_CATEGORIES.forEachIndexed { idx, cat ->
                        FilterChip(
                            selected = categoryIdx == idx,
                            onClick = { categoryIdx = idx },
                            label = { Text(cat.title) }
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { s ->
                                inputText = s.filter { it.isDigit() || it == '.' || it == '-' }
                            },
                            label = { Text("Value to Convert") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("converter_value_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UnitDropdown(
                                label = "From",
                                units = category.units,
                                selectedIdx = fromIdx,
                                onSelect = { fromIdx = it },
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalIconButton(
                                onClick = {
                                    val temp = fromIdx
                                    fromIdx = toIdx
                                    toIdx = temp
                                },
                                modifier = Modifier.testTag("converter_swap_button")
                            ) {
                                Icon(Icons.Rounded.SwapHoriz, contentDescription = "Swap units")
                            }
                            UnitDropdown(
                                label = "To",
                                units = category.units,
                                selectedIdx = toIdx,
                                onSelect = { toIdx = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Surface(
                            color = CyanAccent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "${formatSmartNumber(inputVal)} ${fromUnit.symbol} =",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${formatSmartNumber(convertedVal)} ${toUnit.symbol}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CyanAccent
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "All ${category.title} Equivalents",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(category.units) { unit ->
                val v = unit.fromBase(baseVal)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(unit.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "${formatSmartNumber(v)} ${unit.symbol}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
    label: String,
    units: List<UnitDef>,
    selectedIdx: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = units[selectedIdx.coerceIn(0, units.lastIndex)]

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "${selected.name} (${selected.symbol})",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            units.forEachIndexed { idx, unit ->
                DropdownMenuItem(
                    text = { Text("${unit.name} (${unit.symbol})") },
                    onClick = {
                        onSelect(idx)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatSmartNumber(value: Double): String {
    if (value == 0.0) return "0"
    val abs = kotlin.math.abs(value)
    return when {
        abs >= 10000 -> String.format(Locale.US, "%,.2f", value)
        abs >= 1 -> String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
        else -> String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
    }
}
