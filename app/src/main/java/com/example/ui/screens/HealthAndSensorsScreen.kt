package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CobaltAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.VioletAccent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun HealthAndSensorsScreen(
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = BMI, 1 = Age, 2 = Compass & Level

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
                        text = "Health, Age & Sensors",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Check BMI & ideal weight, calculate exact age, or use live hardware Compass & Level.",
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
                        icon = { Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("health_tab_bmi")
                    ) { Text("BMI") }
                    SegmentedButton(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(1, 3),
                        icon = { Icon(Icons.Rounded.Cake, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("health_tab_age")
                    ) { Text("Age") }
                    SegmentedButton(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        shape = SegmentedButtonDefaults.itemShape(2, 3),
                        icon = { Icon(Icons.Rounded.Explore, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("health_tab_compass")
                    ) { Text("Compass") }
                }
            }

            when (activeTab) {
                0 -> item { BmiCalculatorPane() }
                1 -> item { AgeCalculatorPane() }
                else -> item { CompassAndLevelPane() }
            }
        }
    }
}

@Composable
private fun BmiCalculatorPane() {
    var heightCm by remember { mutableFloatStateOf(172f) }
    var weightKg by remember { mutableFloatStateOf(68f) }
    var ageYears by remember { mutableFloatStateOf(26f) }

    val heightM = (heightCm / 100f).toDouble()
    val bmi = weightKg / heightM.pow(2.0)
    val minHealthy = 18.5 * heightM.pow(2.0)
    val maxHealthy = 24.9 * heightM.pow(2.0)
    val bmr = (10 * weightKg + 6.25 * heightCm - 5 * ageYears + 5).roundToInt()

    val (category, accent) = when {
        bmi < 18.5 -> "Underweight" to CobaltAccent
        bmi < 25.0 -> "Healthy Weight" to EmeraldAccent
        bmi < 30.0 -> "Overweight" to AmberAccent
        else -> "Obese" to RoseAccent
    }

    val gaugeRatio = ((bmi - 12.0) / (40.0 - 12.0)).toFloat().coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 210.dp, height = 118.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 18.dp.toPx()
                        drawArc(
                            color = Color.Gray.copy(alpha = 0.2f),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = accent,
                            startAngle = 180f,
                            sweepAngle = 180f * gaugeRatio,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.US, "%.1f", bmi),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            color = accent
                        )
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleMedium,
                            color = accent
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ideal Range", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "${minHealthy.roundToInt()}–${maxHealthy.roundToInt()} kg",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Daily BMR", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "$bmr kcal",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily)
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SliderMetric("Height", "${heightCm.roundToInt()} cm", heightCm, 120f..220f) { heightCm = it }
                SliderMetric("Weight", "${weightKg.roundToInt()} kg", weightKg, 30f..160f) { weightKg = it }
                SliderMetric("Age", "${ageYears.roundToInt()} yrs", ageYears, 10f..90f) { ageYears = it }
            }
        }
    }
}

@Composable
private fun SliderMetric(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                valueText,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgeCalculatorPane() {
    val defaultBirth = remember {
        Calendar.getInstance().apply { set(1998, Calendar.MAY, 15, 0, 0, 0) }.timeInMillis
    }
    var birthMillis by remember { mutableLongStateOf(defaultBirth) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dobCal = remember(birthMillis) {
        Calendar.getInstance().apply { timeInMillis = birthMillis }
    }
    val nowCal = remember { Calendar.getInstance() }

    val ageBreakdown = remember(birthMillis) {
        var years = nowCal.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
        var months = nowCal.get(Calendar.MONTH) - dobCal.get(Calendar.MONTH)
        var days = nowCal.get(Calendar.DAY_OF_MONTH) - dobCal.get(Calendar.DAY_OF_MONTH)

        if (days < 0) {
            months -= 1
            val prevMonth = (nowCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            days += prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
        if (months < 0) {
            years -= 1
            months += 12
        }

        val totalDays = ((nowCal.timeInMillis - dobCal.timeInMillis) / (1000L * 60 * 60 * 24)).coerceAtLeast(0L)
        val nextBday = (dobCal.clone() as Calendar).apply {
            set(Calendar.YEAR, nowCal.get(Calendar.YEAR))
            if (before(nowCal)) add(Calendar.YEAR, 1)
        }
        val daysToNextBday = ((nextBday.timeInMillis - nowCal.timeInMillis) / (1000L * 60 * 60 * 24)).coerceAtLeast(0L)
        Triple(Triple(years.coerceAtLeast(0), months, days), totalDays, daysToNextBday)
    }

    val dateLabel = remember(birthMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(birthMillis))
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = birthMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { birthMillis = it }
                        showDatePicker = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Date of Birth", style = MaterialTheme.typography.labelMedium)
                    Text(dateLabel, style = MaterialTheme.typography.titleLarge)
                }
                Button(onClick = { showDatePicker = true }) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Change Date")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = VioletAccent.copy(alpha = 0.12f)),
            border = BorderStroke(1.dp, VioletAccent.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val (ymd, totalDays, nextBdayDays) = ageBreakdown
                Text("Exact Age", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "${ymd.first} yrs • ${ymd.second} mo • ${ymd.third} days",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = VioletAccent
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Days Lived", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "$totalDays days",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Next Birthday In", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "$nextBdayDays days",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                        color = EmeraldAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun CompassAndLevelPane() {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    var azimuthDeg by remember { mutableFloatStateOf(0f) }
    var rollDeg by remember { mutableFloatStateOf(0f) }
    var pitchDeg by remember { mutableFloatStateOf(0f) }
    var hasSensors by remember { mutableStateOf(true) }

    DisposableEffect(sensorManager) {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (accel == null) {
            hasSensors = false
        }

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        val rMat = FloatArray(9)
        val iMat = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val alpha = 0.85f
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    for (i in 0..2) gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i]
                    pitchDeg = Math.toDegrees(kotlin.math.atan2(gravity[1].toDouble(), gravity[2].toDouble())).toFloat()
                    rollDeg = Math.toDegrees(kotlin.math.atan2(gravity[0].toDouble(), gravity[2].toDouble())).toFloat()
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    for (i in 0..2) geomagnetic[i] = alpha * geomagnetic[i] + (1 - alpha) * event.values[i]
                }
                if (magnet != null && SensorManager.getRotationMatrix(rMat, iMat, gravity, geomagnetic)) {
                    SensorManager.getOrientation(rMat, orientation)
                    val deg = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
                    azimuthDeg = deg
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        accel?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        magnet?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val cardinal = when (((azimuthDeg + 22.5f) % 360f).toInt() / 45) {
        0 -> "N"
        1 -> "NE"
        2 -> "E"
        3 -> "SE"
        4 -> "S"
        5 -> "SW"
        6 -> "W"
        else -> "NW"
    }
    val isLevel = abs(rollDeg) < 2.0f && abs(pitchDeg) < 2.0f

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${azimuthDeg.roundToInt()}° $cardinal",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = JetBrainsMonoFamily
                ),
                color = CobaltAccent
            )

            Box(
                modifier = Modifier.size(210.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f - 10.dp.toPx()
                    drawCircle(
                        color = CobaltAccent.copy(alpha = 0.16f),
                        radius = radius,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    rotate(degrees = -azimuthDeg) {
                        // North needle
                        drawLine(
                            color = RoseAccent,
                            start = center,
                            end = Offset(center.x, center.y - radius + 16.dp.toPx()),
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // South needle
                        drawLine(
                            color = CobaltAccent,
                            start = center,
                            end = Offset(center.x, center.y + radius - 26.dp.toPx()),
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    // Bubble level dot in center
                    val bx = (center.x + (rollDeg.coerceIn(-30f, 30f) / 30f) * (radius * 0.45f))
                    val by = (center.y + (pitchDeg.coerceIn(-30f, 30f) / 30f) * (radius * 0.45f))
                    drawCircle(
                        color = if (isLevel) EmeraldAccent else AmberAccent,
                        radius = 12.dp.toPx(),
                        center = Offset(bx, by)
                    )
                }
            }

            Surface(
                color = if (isLevel) EmeraldAccent.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (!hasSensors) {
                        "Hardware compass sensor unavailable on this device"
                    } else if (isLevel) {
                        "Surface is LEVEL (Roll ${rollDeg.roundToInt()}°, Pitch ${pitchDeg.roundToInt()}°)"
                    } else {
                        "Spirit Level: Roll ${rollDeg.roundToInt()}° • Pitch ${pitchDeg.roundToInt()}°"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isLevel) EmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
