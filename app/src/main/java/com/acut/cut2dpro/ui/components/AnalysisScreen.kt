package com.acut.cut2dpro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import com.acut.cut2dpro.domain.algorithm.LayoutResult
import com.acut.cut2dpro.domain.model.ChecklistEntry
import com.acut.cut2dpro.domain.model.CutPart
import com.acut.cut2dpro.domain.model.MaterialSummary
import com.acut.cut2dpro.domain.model.buildChecklistEntries
import com.acut.cut2dpro.domain.model.calculateMaterialSummary
import com.acut.cut2dpro.domain.model.describeOperations
import com.acut.cut2dpro.domain.model.formatDimValue
import com.acut.cut2dpro.domain.model.formatFaDecimal
import com.acut.cut2dpro.domain.model.formatFaInt
import com.acut.cut2dpro.domain.model.toJson
import com.acut.cut2dpro.ui.MainUiState
import com.acut.cut2dpro.ui.theme.AmberGroove
import com.acut.cut2dpro.ui.theme.AmberGrooveSoft
import com.acut.cut2dpro.ui.theme.AmberGrooveText
import com.acut.cut2dpro.ui.theme.AmberGrooveTint
import com.acut.cut2dpro.ui.theme.EmeraldAction
import com.acut.cut2dpro.ui.theme.EmeraldSoft
import com.acut.cut2dpro.ui.theme.EmeraldText
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
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalysisScreen(
    state: MainUiState,
    onRecalculate: () -> Unit,
    onGoToSetup: () -> Unit,
    onZoomChange: (Int) -> Unit,
    onToggleChecklistCode: (String) -> Unit,
    onShowNotice: (String) -> Unit,
) {
    val layoutState = state.layoutState
    if (layoutState == null) {
        EmptyLayoutStateCard(
            canCalculate = state.canCalculate,
            onCalculate = onRecalculate,
            onGoToSetup = onGoToSetup,
        )
        return
    }

    val result = layoutState.result
    val partsSnapshot = layoutState.partsSnapshot
    val isStale = state.isStale
    val checklistEntries = remember(result, partsSnapshot) { buildChecklistEntries(result, partsSnapshot) }
    val materialSummary = remember(partsSnapshot) { calculateMaterialSummary(partsSnapshot) }
    var showJson by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Status & Recalculate bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = if (isStale) AmberGrooveTint else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (isStale) AmberGroove else Slate200),
            shadowElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (isStale) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (isStale) AmberGroove else EmeraldAction,
                    )
                    Column {
                        Text(
                            text = if (isStale) "داده‌ها تغییر کرده‌اند — این نتیجه قدیمی است" else "تحلیل و نقشهٔ برش به‌روز است ✓",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isStale) AmberGrooveText else EmeraldText,
                        )
                        Text(
                            text = "ورق خام ${formatDimValue(result.stock.length)} × ${formatDimValue(result.stock.width)} mm · تیغه ${formatDimValue(result.kerf)} mm · نوار ${formatDimValue(result.bandThickness)} mm",
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate600,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onRecalculate,
                    enabled = state.canCalculate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isStale) AmberGroove else EmeraldAction,
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("analysis_recalculate_button"),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isStale) "محاسبهٔ مجدد" else "بازمحاسبه")
                }
            }
        }

        // KPI Result Cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AnalysisKpiCard(
                    label = "تعداد ورق خام",
                    value = "${formatFaInt(result.summary.sheetCount)} ورق",
                    accentColor = SkyPrimaryDark,
                    containerColor = SkySoft,
                    modifier = Modifier.weight(1f),
                )
                AnalysisKpiCard(
                    label = "بهره‌وری کل",
                    value = "${formatFaDecimal(result.summary.utilizationPercentage)}٪",
                    accentColor = EmeraldText,
                    containerColor = EmeraldSoft,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AnalysisKpiCard(
                    label = "درصد پرت کل",
                    value = "${formatFaDecimal(result.summary.wastePercentage)}٪",
                    accentColor = AmberGrooveText,
                    containerColor = AmberGrooveSoft,
                    modifier = Modifier.weight(1f),
                )
                AnalysisKpiCard(
                    label = "قطعات چیده‌شده",
                    value = "${formatFaInt(result.summary.placedCount)} از ${formatFaInt(result.summary.totalParts)}",
                    accentColor = Slate900,
                    containerColor = Slate100,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Per-sheet summary chips + JSON toggle
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Slate200),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "خلاصهٔ هر ورق",
                        style = MaterialTheme.typography.titleSmall,
                        color = Slate900,
                    )
                    TextButton(
                        onClick = { showJson = !showJson },
                        modifier = Modifier.testTag("toggle_json_viewer_button"),
                    ) {
                        Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (showJson) "بستن JSON" else "نمایش JSON")
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    result.sheets.forEach { sheet ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SubtleSurface,
                            border = BorderStroke(1.dp, Slate200),
                        ) {
                            Text(
                                text = "ورق ${formatFaInt(sheet.sheetIndex + 1)}: ${formatFaInt(sheet.placements.size)} قطعه · بهره‌وری ${formatFaDecimal(sheet.utilizationPercentage)}٪ · پرت ${formatFaDecimal(sheet.wastePercentage)}٪",
                                style = MaterialTheme.typography.labelMedium,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = showJson) {
                    JsonResultBox(
                        result = result,
                        onCopied = { onShowNotice("خروجی JSON در حافظه کپی شد ✓") },
                    )
                }
            }
        }

        // Unplaced parts warning
        if (result.unplaced.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = RoseFarsiTint,
                border = BorderStroke(1.5.dp, RoseFarsi),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = RoseFarsi)
                        Text(
                            text = "${formatFaInt(result.unplaced.size)} قطعه بزرگ‌تر از ورق خام است و چیده نشد",
                            style = MaterialTheme.typography.titleSmall,
                            color = RoseFarsiText,
                        )
                    }
                    result.unplaced.forEach { u ->
                        Text(
                            text = "• ${u.name.ifBlank { "بدون نام" }} (${formatDimValue(u.length)} × ${formatDimValue(u.width)} mm)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RoseFarsiText,
                        )
                    }
                }
            }
        }

        // Interactive B&W Cutting Map
        CuttingMapSection(
            result = result,
            parts = partsSnapshot,
            zoomPercent = state.mapZoomPercent,
            onZoomChange = onZoomChange,
        )

        // Production Control Sheet (Checklist + Material Invoice)
        ControlSheetSection(
            entries = checklistEntries,
            checkedCodes = state.checkedCodes,
            material = materialSummary,
            onToggleCode = onToggleChecklistCode,
        )
    }
}

@Composable
private fun EmptyLayoutStateCard(
    canCalculate: Boolean,
    onCalculate: () -> Unit,
    onGoToSetup: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Slate200),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "هنوز چیدمانی محاسبه نشده است",
                style = MaterialTheme.typography.titleMedium,
                color = Slate900,
            )
            Text(
                text = "پس از وارد کردن ابعاد ورق و لیست قطعات، دکمهٔ «محاسبه چیدمان» را بزنید تا نقشهٔ برش و آمار پرت نمایش داده شود.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate600,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (canCalculate) {
                    Button(
                        onClick = onCalculate,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAction),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("empty_analysis_calculate_button"),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("محاسبه چیدمان")
                    }
                }
                OutlinedButton(
                    onClick = onGoToSetup,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("empty_analysis_back_setup_button"),
                ) {
                    Text("بازگشت به تعریف قطعات")
                }
            }
        }
    }
}

@Composable
private fun AnalysisKpiCard(
    label: String,
    value: String,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, Slate200),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Slate600,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor,
            )
        }
    }
}

@Composable
private fun JsonResultBox(
    result: LayoutResult,
    onCopied: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val jsonString = remember(result) {
        JSONObject().apply {
            put("stock", JSONObject().apply {
                put("length", result.stock.length)
                put("width", result.stock.width)
            })
            put("kerf", result.kerf)
            put("bandThickness", result.bandThickness)
            put("allowRotation", result.allowRotation)
            put("summary", JSONObject().apply {
                put("sheetCount", result.summary.sheetCount)
                put("totalParts", result.summary.totalParts)
                put("placedCount", result.summary.placedCount)
                put("unplacedCount", result.summary.unplacedCount)
                put("utilizationPercentage", result.summary.utilizationPercentage)
                put("wastePercentage", result.summary.wastePercentage)
            })
            put("placements", JSONArray().apply {
                result.placements.forEach { put(it.toJson()) }
            })
        }.toString(2)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate900, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "خروجی JSON چیدمان",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(jsonString))
                    onCopied()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Slate600),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("copy_json_button"),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("کپی JSON", style = MaterialTheme.typography.labelMedium)
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = jsonString.take(2500) + if (jsonString.length > 2500) "\n..." else "",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Color(0xFFCBD5E1),
            )
        }
    }
}

@Composable
private fun ControlSheetSection(
    entries: List<ChecklistEntry>,
    checkedCodes: Set<String>,
    material: MaterialSummary,
    onToggleCode: (String) -> Unit,
) {
    val doneCount = entries.count { checkedCodes.contains(it.code) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Checklist container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Slate200),
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "برگهٔ کنترل تولید و چک‌لیست",
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate900,
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (doneCount == entries.size && entries.isNotEmpty()) EmeraldSoft else SkySoft,
                    ) {
                        Text(
                            text = "${formatFaInt(doneCount)} از ${formatFaInt(entries.size)} انجام‌شده",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (doneCount == entries.size && entries.isNotEmpty()) EmeraldText else SkyPrimaryDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                HorizontalDivider(color = Slate200)

                entries.forEach { entry ->
                    val isDone = checkedCodes.contains(entry.code)
                    val p = entry.placement
                    val source = entry.source
                    val name = (p.name.ifBlank { source?.name ?: "" }).trim().ifEmpty { "بدون نام" }
                    val dims = "${formatDimValue(source?.length ?: p.width)} × ${formatDimValue(source?.width ?: p.height)} mm"

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onToggleCode(entry.code) }
                            .testTag("checklist_item_${entry.code}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDone) EmeraldSoft.copy(alpha = 0.45f) else SubtleSurface,
                        border = BorderStroke(1.dp, if (isDone) EmeraldAction else Slate200),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDone) EmeraldAction else Color.White)
                                    .border(
                                        1.5.dp,
                                        if (isDone) EmeraldAction else Slate400,
                                        RoundedCornerShape(8.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isDone) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "تکمیل شده",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Slate800,
                                    ) {
                                        Text(
                                            text = entry.code,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isDone) Slate500 else Slate900,
                                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                    )
                                    Text(
                                        text = "ورق ${formatFaInt(p.sheetIndex + 1)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Slate500,
                                    )
                                }
                                Text(
                                    text = "$dims  ·  ${describeOperations(source)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate600,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Material Invoice
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Slate200),
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "فاکتور متریال مصرفی (تخمینی)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MaterialStatPill(
                        title = "نوار لبه",
                        value = "${formatFaDecimal(material.bandMeters)} متر",
                        tint = TealBandTint,
                        border = TealBandSoft,
                        textColor = TealBandText,
                        modifier = Modifier.weight(1f),
                    )
                    MaterialStatPill(
                        title = "فارسی ۴۵°",
                        value = "${formatFaDecimal(material.farsiMeters)} متر",
                        tint = RoseFarsiTint,
                        border = RoseFarsiSoft,
                        textColor = RoseFarsiText,
                        modifier = Modifier.weight(1f),
                    )
                    MaterialStatPill(
                        title = "شیار",
                        value = "${formatFaDecimal(material.grooveMeters)} متر",
                        tint = AmberGrooveTint,
                        border = AmberGrooveSoft,
                        textColor = AmberGrooveText,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialStatPill(
    title: String,
    value: String,
    tint: Color,
    border: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = tint,
        border = BorderStroke(1.dp, border),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = Slate600)
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = textColor,
            )
        }
    }
}
