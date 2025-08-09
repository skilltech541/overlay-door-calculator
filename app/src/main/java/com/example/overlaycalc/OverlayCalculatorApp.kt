@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.overlaycalc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
    val perSideOverlay16ths =
        if (selectedOverlay.value16ths >= 0) selectedOverlay.value16ths else customOverlay16ths

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
                    Icon(
                        painter = painterResource(id = R.drawable.nhance_logo),
                        contentDescription = "N-Hance"
                    )
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Enter opening size. Overlay is applied per side (width: left+right, height: top+bottom).")

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FieldCard(
                    title = "Opening Width",
                    input = width,
                    selected = (activeField == ActiveField.WIDTH),
                    onSelect = { activeField = ActiveField.WIDTH },
                    modifier = Modifier.weight(1f)
                )
                FieldCard(
                    title = "Opening Height",
                    input = height,
                    selected = (activeField == ActiveField.HEIGHT),
                    onSelect = { activeField = ActiveField.HEIGHT },
                    modifier = Modifier.weight(1f)
                )
            }

            // Keypad ABOVE overlay (as requested)
            Keypad(
                onDigit = { d ->
                    when (activeField) {
                        ActiveField.WIDTH -> width = width.addDigit(d)
                        ActiveField.HEIGHT -> height = height.addDigit(d)
                    }
                },
                onFraction = { f16 ->
                    when (activeField) {
                        ActiveField.WIDTH -> width = width.setFraction(f16)
                        ActiveField.HEIGHT -> height = height.setFraction(f16)
                    }
                },
                onBackspace = {
                    when (activeField) {
                        ActiveField.WIDTH -> width = width.backspace()
                        ActiveField.HEIGHT -> height = height.backspace()
                    }
                },
                onClear = {
                    when (activeField) {
                        ActiveField.WIDTH -> width = MeasureInput()
                        ActiveField.HEIGHT -> height = MeasureInput()
                    }
                },
                onSwap = {
                    activeField =
                        if (activeField == ActiveField.WIDTH) ActiveField.HEIGHT else ActiveField.WIDTH
                }
            )

            Divider()

            OverlaySelector(
                options = overlayOptions,
                selected = selectedOverlay,
                onSelected = { selectedOverlay = it },
                customValue16ths = customOverlay16ths,
                onCustomChanged = { customOverlay16ths = it }
            )

            SplitSelector(
                splitDoors = splitDoors,
                onToggle = { splitDoors = it },
                centerGap16ths = centerGap16ths,
                onGapChanged = { centerGap16ths = it }
            )

            ResultCard(finished = finished, perDoor = perDoor)

            Divider()
            Text("Formula: finished = opening + 2 × overlay (per side). Rounded to nearest 1/16\".")
        }
    }
}

enum class ActiveField { WIDTH, HEIGHT }
data class OverlayOption(val label: String, val value16ths: Int)

@Composable
fun FieldCard(
    title: String,
    input: MeasureInput,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(onClick = onSelect, modifier = modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(
                input.formatted(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            AssistChip(
                onClick = onSelect,
                label = { Text(if (selected) "Active" else "Tap to edit") }
            )
        }
    }
}

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
                    value = if (selected.value16ths >= 0)
                        selected.label
                    else
                        "Custom: ${format16ths(customValue16ths)
