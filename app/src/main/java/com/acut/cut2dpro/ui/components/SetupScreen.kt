package com.acut.cut2dpro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acut.cut2dpro.domain.model.CutPart
import com.acut.cut2dpro.domain.model.LayoutSettings
import com.acut.cut2dpro.domain.model.STOCK_PRESETS
import com.acut.cut2dpro.domain.model.formatDimValue
import com.acut.cut2dpro.domain.model.formatFaDecimal
import com.acut.cut2dpro.domain.model.formatFaInt
import com.acut.cut2dpro.domain.model.isEmptyPart
import com.acut.cut2dpro.domain.model.normalizeEdgeCount
import com.acut.cut2dpro.domain.model.parseDimensionInput
import com.acut.cut2dpro.domain.model.parseQtyInput
import com.acut.cut2dpro.ui.MainUiState
import com.acut.cut2dpro.ui.theme.AmberGroove
import com.acut.cut2dpro.ui.theme.AmberGrooveSoft
import com.acut.cut2dpro.ui.theme.AmberGrooveText
import com.acut.cut2dpro.ui.theme.AmberGrooveTint
import com.acut.cut2dpro.ui.theme.EmeraldAction
import com.acut.cut2dpro.ui.theme.RoseFarsi
import com.acut.cut2dpro.ui.theme.RoseFarsiSoft
import com.acut.cut2dpro.ui.theme.RoseFarsiText
import com.acut.cut2dpro.ui.theme.RoseFarsiTint
import com.acut.cut2dpro.ui.theme.SkyPrimaryDark
import com.acut.cut2dpro.ui.theme.SkySoft
import com.acut.cut2dpro.ui.theme.SkySoftBorder
import com.acut.cut2dpro.ui.theme.Slate100
import com.acut.cut2dpro.ui.theme.Slate200
import com.acut.cut2dpro.ui.theme.Slate300
import com.acut.cut2dpro.ui.theme.Slate400
import com.acut.cut2dpro.ui.theme.Slate500
import com.acut.cut2dpro.ui.theme.Slate600
import com.acut.cut2dpro.ui.theme.Slate700
import com.acut.cut2dpro.ui.theme.Slate800
import com.acut.cut2dpro.ui.theme.Slate900
import com.acut.cut2dpro.ui.theme.SubtleSurface
import com.acut.cut2dpro.ui.theme.TealBand
import com.acut.cut2dpro.ui.theme.TealBandSoft
import com.acut.cut2dpro.ui.theme.TealBandText
import com.acut.cut2dpro.ui.theme.TealBandTint

@Composable
fun SetupScreen(
    state: MainUiState,
    onUpdateSettings: ((LayoutSettings) -> LayoutSettings) -> Unit,
    onCalculate: () -> Unit,
    onSetActivePart: (String) -> Unit,
    onClearFocusTarget: () -> Unit,
    onUpdatePart: (String, (CutPart) -> CutPart) -> Unit,
    onInsertRowAfter: (String?, CutPart?) -> Unit,
    onDeleteRow: (String) -> Unit,
    onLoadSample: () -> Unit,
    onRemoveEmpty: () -> Unit,
    onClearAll: () -> Unit,
) {
    var confirmSampleDialog by remember { mutableStateOf(false) }
    var confirmClearDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 1) Live Stats Cards
        StatsSummaryGrid(state)

        // Incomplete rows warning
        if (state.stats.incomplete > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = RoseFarsiTint,
                border = BorderStroke(1.dp, RoseFarsiSoft),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(12.dp),
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RoseFarsi, modifier = Modifier.size(18.dp))
                    Text(
                        text = "${formatFaInt(state.stats.incomplete)} ردیف ناقص است (ابعاد آن کامل نشده) و در محاسبات بالا لحاظ نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoseFarsiText,
                    )
                }
            }
        }

        // 2) Stock Sheet, Kerf, Band Thickness & Calculate Card
        StockSettingsCard(
            settings = state.settings,
            canCalculate = state.canCalculate,
            hasValidParts = state.validParts.isNotEmpty(),
            error = state.layoutError,
            onUpdateSettings = onUpdateSettings,
            onCalculate = onCalculate,
        )

        // 3) Parts Data Entry Section
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Slate200),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                // Toolbar
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SubtleSurface)
                        .padding(12.dp),
                ) {
                    Button(
                        onClick = { onInsertRowAfter(state.activePartId ?: state.rows.lastOrNull()?.id, null) },
                        colors = ButtonDefaults.buttonColors(containerColor = SkyPrimaryDark),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("add_row_button"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("افزودن ردیف", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = {
                            if (state.rows.any { !isEmptyPart(it) }) {
                                confirmSampleDialog = true
                            } else {
                                onLoadSample()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Slate300),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("sample_data_button"),
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp), tint = Slate700)
                        Spacer(Modifier.width(4.dp))
                        Text("دادهٔ نمونه", color = Slate700, style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = onRemoveEmpty,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Slate300),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("remove_empty_button"),
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp), tint = Slate700)
                        Spacer(Modifier.width(4.dp))
                        Text("حذف خالی‌ها", color = Slate700, style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = { confirmClearDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, RoseFarsiSoft),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("clear_all_button"),
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoseFarsiText)
                        Spacer(Modifier.width(4.dp))
                        Text("پاک‌کردن همه", color = RoseFarsiText, style = MaterialTheme.typography.labelLarge)
                    }
                }
                HorizontalDivider(color = Slate200)

                // Parts List
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(12.dp),
                ) {
                    state.rows.forEachIndexed { index, part ->
                        PartEditorCard(
                            index = index,
                            part = part,
                            isActive = part.id == state.activePartId,
                            canDelete = state.rows.size > 1,
                            requestNameFocus = state.focusTarget?.first == part.id && state.focusTarget.second == "name",
                            onClearFocusTarget = onClearFocusTarget,
                            onActivate = { onSetActivePart(part.id) },
                            onUpdate = { transform -> onUpdatePart(part.id, transform) },
                            onDuplicate = { onInsertRowAfter(part.id, part) },
                            onDelete = { onDeleteRow(part.id) },
                            onFinishRowNext = { onInsertRowAfter(part.id, null) },
                        )
                    }
                }

                HorizontalDivider(color = Slate200)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SubtleSurface)
                        .clickable { onInsertRowAfter(state.rows.lastOrNull()?.id, null) }
                        .padding(14.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = SkyPrimaryDark, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "افزودن ردیف جدید",
                        style = MaterialTheme.typography.titleSmall,
                        color = SkyPrimaryDark,
                    )
                }
            }
        }

        // 4) Live Part Preview & Edge Legend (responsive side-by-side on tablets)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 760.dp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PartPreviewCard(part = state.activePart, modifier = Modifier.weight(3f))
                    EdgeLegendGuideCard(modifier = Modifier.weight(2f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PartPreviewCard(part = state.activePart, modifier = Modifier.fillMaxWidth())
                    EdgeLegendGuideCard(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (confirmSampleDialog) {
        AlertDialog(
            onDismissRequest = { confirmSampleDialog = false },
            title = { Text("جایگزینی با دادهٔ نمونه") },
            text = { Text("داده‌های فعلی با دادهٔ نمونه جایگزین شود؟") },
            confirmButton = {
                Button(onClick = {
                    confirmSampleDialog = false
                    onLoadSample()
                }) { Text("جایگزین کن") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSampleDialog = false }) { Text("انصراف") }
            },
        )
    }

    if (confirmClearDialog) {
        AlertDialog(
            onDismissRequest = { confirmClearDialog = false },
            title = { Text("پاک‌کردن همهٔ ردیف‌ها") },
            text = { Text("همهٔ ردیف‌ها حذف شوند؟") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmClearDialog = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseFarsi),
                ) { Text("حذف همه") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearDialog = false }) { Text("انصراف") }
            },
        )
    }
}

@Composable
private fun StatsSummaryGrid(state: MainUiState) {
    val stats = state.stats
    val items = listOf(
        Triple("ردیف معتبر", formatFaInt(stats.rowCount), "دارای ابعاد کامل" to Slate800),
        Triple("مجموع قطعات", formatFaInt(stats.totalQty), "با احتساب تعداد" to Slate800),
        Triple("مساحت کل", "${formatFaDecimal(stats.totalAreaM2)} m²", "متر مربع" to Slate800),
        Triple("نوار لبه (تقریبی)", "${formatFaDecimal(stats.bandMeters)} m", "انتخاب ۰/۱/۲ لبه" to TealBandText),
        Triple("لبهٔ فارسی ۴۵°", formatFaInt(stats.farsiEdges), "علامت «ف» روی نقشه" to RoseFarsiText),
        Triple("شیار (تقریبی)", "${formatFaDecimal(stats.grooveMeters)} m", "شیار ورق ۳ میل" to AmberGrooveText),
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cols = when {
            maxWidth >= 960.dp -> 6
            maxWidth >= 600.dp -> 3
            else -> 2
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(cols).forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    rowItems.forEach { (title, value, hintAndColor) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(title, style = MaterialTheme.typography.labelMedium, color = Slate500)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                                    color = hintAndColor.second,
                                )
                                Text(hintAndColor.first, style = MaterialTheme.typography.labelSmall, color = Slate400)
                            }
                        }
                    }
                    repeat(cols - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StockSettingsCard(
    settings: LayoutSettings,
    canCalculate: Boolean,
    hasValidParts: Boolean,
    error: String?,
    onUpdateSettings: ((LayoutSettings) -> LayoutSettings) -> Unit,
    onCalculate: () -> Unit,
) {
    var lenText by remember(settings.stockLength) { mutableStateOf(formatDimValue(settings.stockLength)) }
    var widText by remember(settings.stockWidth) { mutableStateOf(formatDimValue(settings.stockWidth)) }
    var kerfText by remember(settings.kerf) { mutableStateOf(if (settings.kerf == 0.0) "0" else formatDimValue(settings.kerf)) }
    var bandThickText by remember(settings.bandThickness) {
        mutableStateOf(if (settings.bandThickness == 0.0) "0" else formatDimValue(settings.bandThickness))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            Text(
                text = "مرحلهٔ ۱ — تنظیم ورق خام، تیغه و نوار",
                style = MaterialTheme.typography.titleSmall,
                color = Slate800,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                NumericField(
                    label = "طول ورق (mm)",
                    value = lenText,
                    onValueChange = {
                        lenText = it
                        onUpdateSettings { s -> s.copy(stockLength = parseDimensionInput(it)) }
                    },
                    placeholder = "3660",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stock_length_input"),
                )
                NumericField(
                    label = "عرض ورق (mm)",
                    value = widText,
                    onValueChange = {
                        widText = it
                        onUpdateSettings { s -> s.copy(stockWidth = parseDimensionInput(it)) }
                    },
                    placeholder = "1830",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stock_width_input"),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                NumericField(
                    label = "ضخامت تیغه Kerf (mm)",
                    value = kerfText,
                    onValueChange = {
                        kerfText = it
                        onUpdateSettings { s -> s.copy(kerf = parseDimensionInput(it)) }
                    },
                    placeholder = "4",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kerf_input"),
                )
                NumericField(
                    label = "ضخامت نوار (mm)",
                    value = bandThickText,
                    onValueChange = {
                        bandThickText = it
                        onUpdateSettings { s -> s.copy(bandThickness = parseDimensionInput(it)) }
                    },
                    placeholder = "0",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("band_thickness_input"),
                )
            }
            Text(
                text = "ضخامت نوار به‌ازای هر لبهٔ نوارخورده به‌صورت خودکار از بُعد عمود کسر می‌شود.",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
            )

            // Presets
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "اندازه‌های رایج:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                STOCK_PRESETS.forEach { preset ->
                    val active = settings.stockLength == preset.length && settings.stockWidth == preset.width
                    Surface(
                        color = if (active) SkyPrimaryDark else Color.White,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (active) SkyPrimaryDark else Slate300),
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                lenText = formatDimValue(preset.length)
                                widText = formatDimValue(preset.width)
                                onUpdateSettings { s -> s.copy(stockLength = preset.length, stockWidth = preset.width) }
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) Color.White else Slate700,
                            )
                        }
                    }
                }
            }

            // Rotation Toggle
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SubtleSurface,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onUpdateSettings { s -> s.copy(allowRotation = !s.allowRotation) } }
                    .testTag("allow_rotation_toggle"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Checkbox(
                        checked = settings.allowRotation,
                        onCheckedChange = { checked -> onUpdateSettings { s -> s.copy(allowRotation = checked) } },
                        colors = CheckboxDefaults.colors(checkedColor = SkyPrimaryDark),
                    )
                    Text(
                        text = "چرخش ۹۰° قطعات مجاز است",
                        style = MaterialTheme.typography.labelLarge,
                        color = Slate700,
                    )
                }
            }

            if (error != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = RoseFarsiTint,
                    border = BorderStroke(1.dp, RoseFarsiSoft),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "⚠ $error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RoseFarsiText,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            Button(
                onClick = onCalculate,
                enabled = canCalculate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldAction,
                    disabledContainerColor = Slate300,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("calculate_layout_button"),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "محاسبهٔ چیدمان (Calculate Layout)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                )
            }

            if (!hasValidParts) {
                Text(
                    text = "ابتدا دست‌کم یک قطعه با طول، عرض و تعداد کامل وارد کنید.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                )
            }
        }
    }
}

@Composable
private fun PartEditorCard(
    index: Int,
    part: CutPart,
    isActive: Boolean,
    canDelete: Boolean,
    requestNameFocus: Boolean,
    onClearFocusTarget: () -> Unit,
    onActivate: () -> Unit,
    onUpdate: ((CutPart) -> CutPart) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onFinishRowNext: () -> Unit,
) {
    val nameFocus = remember { FocusRequester() }
    val lenFocus = remember { FocusRequester() }
    val widFocus = remember { FocusRequester() }
    val qtyFocus = remember { FocusRequester() }

    LaunchedEffect(requestNameFocus) {
        if (requestNameFocus) {
            runCatching { nameFocus.requestFocus() }
            onClearFocusTarget()
        }
    }

    var lenText by remember(part.id, part.length) { mutableStateOf(formatDimValue(part.length)) }
    var widText by remember(part.id, part.width) { mutableStateOf(formatDimValue(part.width)) }
    var qtyText by remember(part.id, part.qty) { mutableStateOf(if (part.qty > 0) part.qty.toString() else "") }

    val started = part.name.trim().isNotEmpty() || part.length > 0.0 || part.width > 0.0
    val invalidLen = started && part.length <= 0.0
    val invalidWid = started && part.width <= 0.0
    val invalidQty = started && part.qty <= 0

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) SkySoft.copy(alpha = 0.35f) else SubtleSurface,
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, if (isActive) SkyPrimaryDark else Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onActivate() }
            .testTag("part_row_card_$index"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(10.dp),
        ) {
            // Row header: number badge + name + duplicate/delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) SkyPrimaryDark else Slate200),
                ) {
                    Text(
                        text = formatFaInt(index + 1),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isActive) Color.White else Slate700,
                    )
                }

                OutlinedTextField(
                    value = part.name,
                    onValueChange = { v -> onUpdate { it.copy(name = v) } },
                    placeholder = { Text("نام قطعه…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { lenFocus.requestFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = SkyPrimaryDark,
                        unfocusedBorderColor = Slate300,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(nameFocus)
                        .onFocusChanged { if (it.isFocused) onActivate() },
                )

                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "تکثیر ردیف", tint = Slate600, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onDelete,
                    enabled = canDelete,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف ردیف",
                        tint = if (canDelete) RoseFarsi else Slate300,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Dimensions + Qty row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                NumericField(
                    label = "طول (mm)",
                    value = lenText,
                    isError = invalidLen,
                    imeAction = ImeAction.Next,
                    onNext = { widFocus.requestFocus() },
                    onFocus = onActivate,
                    onValueChange = { raw ->
                        lenText = raw
                        onUpdate { it.copy(length = parseDimensionInput(raw)) }
                    },
                    placeholder = "0",
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(lenFocus),
                )
                NumericField(
                    label = "عرض (mm)",
                    value = widText,
                    isError = invalidWid,
                    imeAction = ImeAction.Next,
                    onNext = { qtyFocus.requestFocus() },
                    onFocus = onActivate,
                    onValueChange = { raw ->
                        widText = raw
                        onUpdate { it.copy(width = parseDimensionInput(raw)) }
                    },
                    placeholder = "0",
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(widFocus),
                )
                NumericField(
                    label = "تعداد",
                    value = qtyText,
                    isError = invalidQty,
                    imeAction = ImeAction.Done,
                    onNext = onFinishRowNext,
                    onFocus = onActivate,
                    isInteger = true,
                    onValueChange = { raw ->
                        qtyText = raw
                        onUpdate { it.copy(qty = parseQtyInput(raw)) }
                    },
                    placeholder = "1",
                    modifier = Modifier
                        .weight(0.75f)
                        .focusRequester(qtyFocus),
                )
            }

            // Edge operation selectors (0 / 1 / 2) grouped by Band, Farsi 45°, Groove
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EdgeFlagGroupBar(
                    title = "نوار (لبه‌چسب)",
                    bgColor = TealBandTint,
                    borderColor = TealBandSoft,
                    accentColor = TealBand,
                    titleColor = TealBandText,
                    lengthCount = normalizeEdgeCount(part.bandLength),
                    widthCount = normalizeEdgeCount(part.bandWidth),
                    onLengthChange = { v -> onActivate(); onUpdate { it.copy(bandLength = v) } },
                    onWidthChange = { v -> onActivate(); onUpdate { it.copy(bandWidth = v) } },
                )
                EdgeFlagGroupBar(
                    title = "فارسی ۴۵°",
                    bgColor = RoseFarsiTint,
                    borderColor = RoseFarsiSoft,
                    accentColor = RoseFarsi,
                    titleColor = RoseFarsiText,
                    lengthCount = normalizeEdgeCount(part.farsiLength),
                    widthCount = normalizeEdgeCount(part.farsiWidth),
                    onLengthChange = { v -> onActivate(); onUpdate { it.copy(farsiLength = v) } },
                    onWidthChange = { v -> onActivate(); onUpdate { it.copy(farsiWidth = v) } },
                )
                EdgeFlagGroupBar(
                    title = "شیار (۳ میل)",
                    bgColor = AmberGrooveTint,
                    borderColor = AmberGrooveSoft,
                    accentColor = AmberGroove,
                    titleColor = AmberGrooveText,
                    lengthCount = normalizeEdgeCount(part.grooveLength),
                    widthCount = normalizeEdgeCount(part.grooveWidth),
                    onLengthChange = { v -> onActivate(); onUpdate { it.copy(grooveLength = v) } },
                    onWidthChange = { v -> onActivate(); onUpdate { it.copy(grooveWidth = v) } },
                )
            }
        }
    }
}

@Composable
private fun EdgeFlagGroupBar(
    title: String,
    bgColor: Color,
    borderColor: Color,
    accentColor: Color,
    titleColor: Color,
    lengthCount: Int,
    widthCount: Int,
    onLengthChange: (Int) -> Unit,
    onWidthChange: (Int) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = titleColor,
                modifier = Modifier.weight(1f),
            )
            EdgeCountSegmented(
                axisLabel = "طول",
                value = lengthCount,
                accentColor = accentColor,
                onSelect = onLengthChange,
            )
            EdgeCountSegmented(
                axisLabel = "عرض",
                value = widthCount,
                accentColor = accentColor,
                onSelect = onWidthChange,
            )
        }
    }
}

@Composable
private fun EdgeCountSegmented(
    axisLabel: String,
    value: Int,
    accentColor: Color,
    onSelect: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = "$axisLabel:",
            style = MaterialTheme.typography.labelSmall,
            color = Slate600,
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Slate300, RoundedCornerShape(8.dp)),
        ) {
            listOf(0, 1, 2).forEach { count ->
                val selected = value == count
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 30.dp, height = 32.dp)
                        .background(if (selected) accentColor else Color.White)
                        .clickable { onSelect(count) },
                ) {
                    Text(
                        text = formatFaInt(count),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = if (selected) Color.White else Slate600,
                    )
                }
            }
        }
    }
}

@Composable
private fun NumericField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    isInteger: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onNext: () -> Unit = {},
    onFocus: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1, fontSize = 11.sp) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        isError = isError,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isInteger) KeyboardType.Number else KeyboardType.Decimal,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() },
            onDone = { onNext() },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = SkyPrimaryDark,
            unfocusedBorderColor = Slate300,
            errorContainerColor = RoseFarsiTint,
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.onFocusChanged { if (it.isFocused) onFocus() },
    )
}
