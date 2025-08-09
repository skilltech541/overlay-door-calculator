package com.example.overlaycalc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OverlayCalculatorApp() {
    var activeField by remember { mutableStateOf(ActiveField.WIDTH) }
    var width by remember { mutableStateOf(MeasureInput()) }
    var height by remember { mutableStateOf(MeasureInput()) }

    val overlayOptions = listOf(
        OverlayOption("1/2\" per side", 8),
        OverlayOption("5/8\" per side", 10),
        OverlayOption("3/4\" per side (default)", 12),
        OverlayOption("1\" per side", 16),
        OverlayOption("Custom...", -1)
    )
    var selectedOverlay by remember { mutableStateOf(overlayOptions[2]) }
    var customOverlay16ths by remember { mutableStateOf(12) }
    val perSideOverlay16ths = if (selectedOverlay.value16ths >= 0) selectedOverlay.value16ths else customOverlay16ths

    val finished = remember(width, height, perSideOverlay16ths) {
        FinishedSize.fromOpening(width, height, perSideOverlay16ths)
    }

    var splitDoors by remember { mutableStateOf(true) }
    var centerGap16ths by remember { mutableStateOf(2) } // 1/8"
    val perDoor = remember(finished, splitDoors, centerGap16ths) {
        if (!splitDoors) null else PerDoor.fromFinished(finished, centerGap16ths)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Overlay Door Calculator", fontWeight = FontWeight.Bold) },
                actions = {
                    Icon(painter = painterResource(id = R.drawable.nhance_logo), contentDescription = "N-Hance")
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Enter opening size. Overlay is applied per side (width: left+right, height: top+bottom).")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FieldCard("Opening Width", width, activeField == ActiveField.WIDTH) { activeField = ActiveField.WIDTH }
                FieldCard("Opening Height", height, activeField == ActiveField.HEIGHT) { activeField = ActiveField.HEIGHT }
            }

            OverlaySelector(
                options = overlayOptions,
                selected = selectedOverlay,
                onSelected = { selectedOverlay = it },
                customValue16ths = customOverlay16ths,
                onCustomChanged = { customOverlay16ths = it }
            )

            SplitSelector(splitDoors = splitDoors, onToggle = { splitDoors = it }, centerGap16ths = centerGap16ths, onGapChanged = { centerGap16ths = it })

            ResultCard(finished = finished, perDoor = perDoor)

            Divider()

            Keypad(
                onDigit = { d -> when (activeField) { ActiveField.WIDTH -> width = width.addDigit(d); ActiveField.HEIGHT -> height = height.addDigit(d) } },
                onFraction = { f16 -> when (activeField) { ActiveField.WIDTH -> width = width.setFraction(f16); ActiveField.HEIGHT -> height = height.setFraction(f16) } },
                onBackspace = { when (activeField) { ActiveField.WIDTH -> width = width.backspace(); ActiveField.HEIGHT -> height = height.backspace() } },
                onClear = { when (activeField) { ActiveField.WIDTH -> width = MeasureInput(); ActiveField.HEIGHT -> height = MeasureInput() } },
                onSwap = { activeField = if (activeField == ActiveField.WIDTH) ActiveField.HEIGHT else ActiveField.WIDTH }
            )
        }
    }
}

enum class ActiveField { WIDTH, HEIGHT }
data class OverlayOption(val label: String, val value16ths: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldCard(title: String, input: MeasureInput, selected: Boolean, onSelect: () -> Unit) {
    ElevatedCard(onClick = onSelect, modifier = Modifier.weight(1f)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(input.formatted(), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            AssistChip(onClick = onSelect, label = { Text(if (selected) "Active" else "Tap to edit") })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlaySelector(
    options: List<OverlayOption>,
    selected: OverlayOption,
    onSelected: (OverlayOption) -> Unit,
    customValue16ths: Int,
    onCustomChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ElevatedCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Overlay (per side)", style = MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                TextField(
                    readOnly = true,
                    value = if (selected.value16ths >= 0) selected.label else "Custom: ${format16ths(customValue16ths)} per side",
                    onValueChange = {},
                    label = { Text("Overlay") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { opt -> DropdownMenuItem(text = { Text(opt.label) }, onClick = { onSelected(opt); expanded = false }) }
                }
            }
            if (selected.value16ths < 0) {
                FractionRow("Custom overlay per side", customValue16ths, onCustomChanged)
            }
        }
    }
}

@Composable
fun SplitSelector(splitDoors: Boolean, onToggle: (Boolean) -> Unit, centerGap16ths: Int, onGapChanged: (Int) -> Unit) {
    ElevatedCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Split into two doors", style = MaterialTheme.typography.labelLarge)
                Switch(checked = splitDoors, onCheckedChange = onToggle)
            }
            if (splitDoors) {
                Text("Center gap (between the pair)", style = MaterialTheme.typography.labelMedium)
                FractionGrid(onFraction = onGapChanged)
                Text("Selected gap: ${format16ths(centerGap16ths)}\"", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ResultCard(finished: FinishedSize, perDoor: PerDoor?) {
    ElevatedCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Finished Door Size", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Width", fontWeight = FontWeight.SemiBold)
                Text(finished.widthFormatted, textAlign = TextAlign.End)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Height", fontWeight = FontWeight.SemiBold)
                Text(finished.heightFormatted, textAlign = TextAlign.End)
            }
            if (perDoor != null) {
                Divider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Per-door Width", fontWeight = FontWeight.SemiBold)
                    Text(perDoor.widthFormatted, textAlign = TextAlign.End)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Center Gap (pair)", fontWeight = FontWeight.SemiBold)
                    Text(formatTotal16ths(perDoor.centerGap16))
                }
            }
            Divider()
            Text("Formula: finished = opening + 2 × overlay (per side). Rounded to nearest 1/16\".")
        }
    }
}

data class MeasureInput(val inches: Int = 0, val fraction16ths: Int = 0, val hasFraction: Boolean = false) {
    fun formatted(): String = buildString {
        append("$inches"); if (hasFraction && fraction16ths != 0) { append(" "); append(format16ths(fraction16ths)) }; append("\"")
    }
    fun addDigit(d: Int): MeasureInput = copy(inches = (inches * 10 + d).coerceAtMost(999))
    fun setFraction(f16: Int): MeasureInput = copy(fraction16ths = f16, hasFraction = true)
    fun backspace(): MeasureInput = if (hasFraction && fraction16ths != 0) copy(fraction16ths = 0, hasFraction = false) else copy(inches = inches / 10)
    fun total16ths(): Int = inches * 16 + (if (hasFraction) fraction16ths else 0)
}

data class FinishedSize(val width16: Int, val height16: Int) {
    val widthFormatted: String get() = formatTotal16ths(width16)
    val heightFormatted: String get() = formatTotal16ths(height16)
    companion object {
        fun fromOpening(width: MeasureInput, height: MeasureInput, perSideOverlay16ths: Int): FinishedSize {
            val totalOverlay = perSideOverlay16ths * 2
            val w = roundToNearest16th(width.total16ths() + totalOverlay)
            val h = roundToNearest16th(height.total16ths() + totalOverlay)
            return FinishedSize(w, h)
        }
    }
}

data class PerDoor(val width16: Int, val centerGap16: Int) {
    val widthFormatted: String get() = formatTotal16ths(width16)
    companion object {
        fun fromFinished(finished: FinishedSize, centerGap16ths: Int): PerDoor {
            val available = (finished.width16 - centerGap16ths).coerceAtLeast(0)
            val perDoor = available / 2
            return PerDoor(perDoor, centerGap16ths)
        }
    }
}

fun gcd(a: Int, b: Int): Int { var x = kotlin.math.abs(a); var y = kotlin.math.abs(b); while (y != 0) { val t = x % y; x = y; y = t }; return if (x == 0) 1 else x }
fun roundToNearest16th(value16ths: Int): Int = value16ths
fun format16ths(frac16: Int): String { val denom = 16; if (frac16 == 0) return "0"; val g = gcd(frac16, denom); val n = frac16 / g; val d = denom / g; return "$n/$d" }
fun formatTotal16ths(total16: Int): String { val inches = total16 / 16; val frac = total16 % 16; return if (frac == 0) "$inches\"" else "$inches ${format16ths(frac)}\"" }

@Composable
fun Keypad(onDigit: (Int) -> Unit, onFraction: (Int) -> Unit, onBackspace: () -> Unit, onClear: () -> Unit, onSwap: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Keypad", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(7,8,9).forEach { d -> Button(onClick = { onDigit(d) }, modifier = Modifier.weight(1f)) { Text("$d") } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(4,5,6).forEach { d -> Button(onClick = { onDigit(d) }, modifier = Modifier.weight(1f)) { Text("$d") } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(1,2,3).forEach { d -> Button(onClick = { onDigit(d) }, modifier = Modifier.weight(1f)) { Text("$d") } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onSwap, modifier = Modifier.weight(1f)) { Text("Swap W/H") }
            Button(onClick = { onDigit(0) }, modifier = Modifier.weight(1f)) { Text("0") }
            OutlinedButton(onClick = onBackspace, modifier = Modifier.weight(1f)) { Text("⌫") }
        }
        Text("Fractions", style = MaterialTheme.typography.labelMedium)
        FractionGrid(onFraction = onFraction)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Clear") }
        }
    }
}

@Composable
fun FractionGrid(onFraction: (Int) -> Unit) {
    val fractions = listOf(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in fractions.chunked(5)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { f -> OutlinedButton(onClick = { onFraction(f) }, modifier = Modifier.weight(1f)) { Text(format16ths(f)) } }
                repeat(5 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun FractionRow(label: String, value16ths: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        FractionGrid(onFraction = onChange)
        Text("Selected: ${format16ths(value16ths)}\" per side", style = MaterialTheme.typography.bodySmall)
    }
}