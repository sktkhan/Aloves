package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun FinanceToolsScreen(
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = EMI, 1 = Discount+Tax, 2 = Split Bill

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
                        text = "Finance & Bill Suite",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Instant loan EMI breakdown, shopping discount + GST math, and group bill split.",
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
                        shape = SegmentedButtonDefaults.itemShape(0, 3),
                        icon = { Icon(Icons.Rounded.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("finance_tab_emi")
                    ) { Text("EMI Loan") }
                    SegmentedButton(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(1, 3),
                        icon = { Icon(Icons.Rounded.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("finance_tab_discount")
                    ) { Text("Discount") }
                    SegmentedButton(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        shape = SegmentedButtonDefaults.itemShape(2, 3),
                        icon = { Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("finance_tab_split")
                    ) { Text("Bill Split") }
                }
            }

            when (activeTab) {
                0 -> item { EmiCalculatorPane() }
                1 -> item { DiscountTaxPane() }
                else -> item { BillSplitPane() }
            }
        }
    }
}

@Composable
private fun EmiCalculatorPane() {
    var principalText by remember { mutableStateOf("500000") }
    var rateSlider by remember { mutableFloatStateOf(9.5f) }
    var yearsSlider by remember { mutableFloatStateOf(5f) }

    val principal = principalText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val annualRate = rateSlider.toDouble()
    val months = (yearsSlider.roundToInt().coerceAtLeast(1)) * 12

    val monthlyRate = annualRate / 12.0 / 100.0
    val emi = if (principal <= 0.0) {
        0.0
    } else if (monthlyRate == 0.0) {
        principal / months
    } else {
        val factor = (1.0 + monthlyRate).pow(months)
        (principal * monthlyRate * factor) / (factor - 1.0)
    }
    val totalPayment = emi * months
    val totalInterest = (totalPayment - principal).coerceAtLeast(0.0)
    val principalFraction = if (totalPayment > 0.0) (principal / totalPayment).toFloat() else 1f
    val fmt = remember { NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 } }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = principalText,
                    onValueChange = { input -> principalText = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Loan Principal Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emi_principal_input")
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Interest Rate (Annual)", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = String.format(Locale.US, "%.1f%%", rateSlider),
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(value = rateSlider, onValueChange = { rateSlider = it }, valueRange = 1f..24f)
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tenure", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "${yearsSlider.roundToInt()} yrs ($months mo)",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(value = yearsSlider, onValueChange = { yearsSlider = it }, valueRange = 1f..30f)
                }
            }
        }

        // Visual Breakdown Card with Donut Chart
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CobaltAccent.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 16.dp.toPx()
                        val sweepPrincipal = 360f * principalFraction.coerceIn(0f, 1f)
                        drawArc(
                            color = AmberAccent,
                            startAngle = -90f + sweepPrincipal,
                            sweepAngle = (360f - sweepPrincipal).coerceAtLeast(0f),
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                        )
                        drawArc(
                            color = CobaltAccent,
                            startAngle = -90f,
                            sweepAngle = sweepPrincipal,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Principal", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${(principalFraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        Text(
                            text = "Monthly EMI",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = fmt.format(emi),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = CobaltAccent
                        )
                    }
                    LegendMetricRow("Principal", fmt.format(principal), CobaltAccent)
                    LegendMetricRow("Total Interest", fmt.format(totalInterest), AmberAccent)
                    LegendMetricRow("Total Payable", fmt.format(totalPayment), EmeraldAccent)
                }
            }
        }
    }
}

@Composable
private fun LegendMetricRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = color, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily)
        )
    }
}

@Composable
private fun DiscountTaxPane() {
    var priceText by remember { mutableStateOf("2500") }
    var discountPct by remember { mutableFloatStateOf(20f) }
    var taxPct by remember { mutableFloatStateOf(5f) }

    val price = priceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val savedAmount = price * (discountPct / 100.0)
    val discountedPrice = (price - savedAmount).coerceAtLeast(0.0)
    val taxAmount = discountedPrice * (taxPct / 100.0)
    val finalPayable = discountedPrice + taxAmount

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    value = priceText,
                    onValueChange = { input -> priceText = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Original Tag Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Discount %", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${discountPct.roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = EmeraldAccent
                        )
                    }
                    Slider(value = discountPct, onValueChange = { discountPct = it }, valueRange = 0f..90f)
                }
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sales Tax / GST %", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${taxPct.roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = AmberAccent
                        )
                    }
                    Slider(value = taxPct, onValueChange = { taxPct = it }, valueRange = 0f..30f)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = EmeraldAccent.copy(alpha = 0.12f)),
            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Final Payable", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = String.format(Locale.US, "%.2f", finalPayable),
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = EmeraldAccent
                )
                LegendMetricRow("You Save", String.format(Locale.US, "%.2f", savedAmount), EmeraldAccent)
                LegendMetricRow("Tax Added", String.format(Locale.US, "%.2f", taxAmount), AmberAccent)
            }
        }
    }
}

@Composable
private fun BillSplitPane() {
    var billText by remember { mutableStateOf("1800") }
    var tipPct by remember { mutableFloatStateOf(10f) }
    var peopleSlider by remember { mutableFloatStateOf(4f) }

    val bill = billText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val people = peopleSlider.roundToInt().coerceAtLeast(1)
    val tipAmount = bill * (tipPct / 100.0)
    val grandTotal = bill + tipAmount
    val perPerson = grandTotal / people

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    value = billText,
                    onValueChange = { input -> billText = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Total Bill Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tip %", style = MaterialTheme.typography.labelLarge)
                        Text("${tipPct.roundToInt()}%", style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily))
                    }
                    Slider(value = tipPct, onValueChange = { tipPct = it }, valueRange = 0f..30f)
                }
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Number of People", style = MaterialTheme.typography.labelLarge)
                        Text("$people", style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily))
                    }
                    Slider(value = peopleSlider, onValueChange = { peopleSlider = it }, valueRange = 1f..20f)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = CobaltAccent.copy(alpha = 0.12f)),
            border = BorderStroke(1.dp, CobaltAccent.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Each Person Pays", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = String.format(Locale.US, "%.2f", perPerson),
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = CobaltAccent
                )
                LegendMetricRow("Tip Total", String.format(Locale.US, "%.2f", tipAmount), AmberAccent)
                LegendMetricRow("Grand Total", String.format(Locale.US, "%.2f", grandTotal), CobaltAccent)
            }
        }
    }
}
