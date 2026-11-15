package com.acut.cut2dpro.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acut.cut2dpro.domain.algorithm.PartSide
import com.acut.cut2dpro.domain.algorithm.PlacedPart
import com.acut.cut2dpro.domain.algorithm.getPartEdgeSides
import com.acut.cut2dpro.domain.model.ChecklistEntry
import com.acut.cut2dpro.domain.model.CutPart
import com.acut.cut2dpro.domain.model.buildChecklistEntries
import com.acut.cut2dpro.domain.model.describeFlagsLabel
import com.acut.cut2dpro.domain.model.formatDimValue
import com.acut.cut2dpro.domain.model.formatFaInt
import com.acut.cut2dpro.ui.MainUiState
import com.acut.cut2dpro.ui.theme.AmberGroove
import com.acut.cut2dpro.ui.theme.AmberGrooveText
import com.acut.cut2dpro.ui.theme.AmberGrooveTint
import com.acut.cut2dpro.ui.theme.EmeraldAction
import com.acut.cut2dpro.ui.theme.RoseFarsi
import com.acut.cut2dpro.ui.theme.Slate100
import com.acut.cut2dpro.ui.theme.Slate200
import com.acut.cut2dpro.ui.theme.Slate300
import com.acut.cut2dpro.ui.theme.Slate400
import com.acut.cut2dpro.ui.theme.Slate500
import com.acut.cut2dpro.ui.theme.Slate600
import com.acut.cut2dpro.ui.theme.Slate800
import com.acut.cut2dpro.ui.theme.Slate900
import com.acut.cut2dpro.ui.theme.SubtleSurface
import com.acut.cut2dpro.ui.theme.TealBand
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabelsScreen(
    state: MainUiState,
    onRecalculate: () -> Unit,
    onGoToSetup: () -> Unit,
) {
    val layoutState = state.layoutState
    if (layoutState == null) {
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
                    text = "لیبلی برای نمایش وجود ندارد",
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900,
                )
                Text(
                    text = "ابتدا در مرحلهٔ اول قطعات را تعریف کرده و چیدمان را محاسبه کنید تا برگهٔ لیبل ۳×۵ تولید شود.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.canCalculate) {
                        Button(
                            onClick = onRecalculate,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAction),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("labels_empty_calculate_button"),
                        ) {
                            Text("محاسبه چیدمان")
                        }
                    }
                    OutlinedButton(
                        onClick = onGoToSetup,
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("بازگشت به تعریف قطعات")
                    }
                }
            }
        }
        return
    }

    val entries = remember(layoutState) {
        buildChecklistEntries(layoutState.result, layoutState.partsSnapshot)
    }
    val pages = remember(entries) {
        if (entries.isEmpty()) listOf(emptyList()) else entries.chunked(15)
    }
    var selectedPage by remember(pages.size) { mutableIntStateOf(0) }
    var compactCardsMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (state.isStale) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = AmberGrooveTint,
                border = BorderStroke(1.dp, AmberGroove),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = AmberGroove)
                        Text(
                            text = "داده‌ها بعد از محاسبه تغییر کرده‌اند؛ لیبل‌ها بر اساس آخرین محاسبه نمایش داده می‌شوند.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmberGrooveText,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onRecalculate,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGroove),
                        modifier = Modifier.height(44.dp),
                    ) {
                        Text("به‌روزرسانی")
                    }
                }
            }
        }

        // Header + Page selector + View toggle
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
                    Column {
                        Text(
                            text = "پیش‌نمایش برگهٔ لیبل قطعات (۳ × ۵)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate900,
                        )
                        Text(
                            text = "${formatFaInt(entries.size)} لیبل در ${formatFaInt(pages.size)} برگهٔ A4",
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate500,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !compactCardsMode,
                            onClick = { compactCardsMode = false },
                            label = { Text("شبکهٔ ۳×۵") },
                            leadingIcon = {
                                Icon(Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("labels_mode_grid_chip"),
                        )
                        FilterChip(
                            selected = compactCardsMode,
                            onClick = { compactCardsMode = true },
                            label = { Text("نمای کارتی") },
                            leadingIcon = {
                                Icon(Icons.Filled.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("labels_mode_cards_chip"),
                        )
                    }
                }

                if (pages.size > 1 && !compactCardsMode) {
                    HorizontalDivider(color = Slate200)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        pages.indices.forEach { pageIdx ->
                            FilterChip(
                                selected = selectedPage == pageIdx,
                                onClick = { selectedPage = pageIdx },
                                label = { Text("برگهٔ ${formatFaInt(pageIdx + 1)}") },
                                modifier = Modifier.testTag("labels_page_chip_$pageIdx"),
                            )
                        }
                    }
                }
            }
        }

        if (compactCardsMode) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                entries.forEach { entry ->
                    LabelCardItem(
                        entry = entry,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            // Exact 3x5 A4 Sheet Grid (horizontally scrollable on narrow phones to keep proportions crisp)
            val pageEntries = pages.getOrElse(selectedPage) { emptyList() }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Slate300),
                shadowElevation = 2.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(14.dp),
                ) {
                    Column(
                        modifier = Modifier.width(840.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        for (rowIdx in 0 until 5) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                for (colIdx in 0 until 3) {
                                    val slot = rowIdx * 3 + colIdx
                                    if (slot < pageEntries.size) {
                                        LabelCardItem(
                                            entry = pageEntries[slot],
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(178.dp),
                                        )
                                    } else {
                                        EmptyLabelSlot(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(178.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLabelSlot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(SubtleSurface, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                color = Slate300,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                ),
            )
        }
        Text(
            text = "جای خالی لیبل",
            style = MaterialTheme.typography.labelMedium,
            color = Slate400,
        )
    }
}

@Composable
private fun LabelCardItem(
    entry: ChecklistEntry,
    modifier: Modifier = Modifier,
) {
    val p = entry.placement
    val source = entry.source
    val name = (p.name.ifBlank { source?.name ?: "" }).trim().ifEmpty { "بدون نام" }
    val totalQty = source?.qty ?: p.instanceIndex

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.2.dp, Slate300),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
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
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = "ابعاد: ${formatDimValue(source?.length ?: p.width)} × ${formatDimValue(source?.width ?: p.height)} mm",
                    style = MaterialTheme.typography.labelLarge,
                    color = Slate800,
                )
                Text(
                    text = "ورق ${formatFaInt(p.sheetIndex + 1)} · نمونه ${formatFaInt(p.instanceIndex)}/${formatFaInt(totalQty)}" +
                        if (p.isRotated) " (چرخیده)" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate500,
                )
                Text(
                    text = "مختصات: x:${formatDimValue(p.x)}  y:${formatDimValue(p.y)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                )

                if (source != null) {
                    Spacer(Modifier.height(2.dp))
                    LabelOpRow(
                        label = "نوار",
                        desc = describeFlagsLabel(source.bandLength, source.bandWidth),
                        dotColor = TealBand,
                        active = source.bandLength + source.bandWidth > 0,
                    )
                    LabelOpRow(
                        label = "فارسی ۴۵°",
                        desc = describeFlagsLabel(source.farsiLength, source.farsiWidth),
                        dotColor = RoseFarsi,
                        active = source.farsiLength + source.farsiWidth > 0,
                    )
                    LabelOpRow(
                        label = "شیار",
                        desc = describeFlagsLabel(source.grooveLength, source.grooveWidth),
                        dotColor = AmberGroove,
                        active = source.grooveLength + source.grooveWidth > 0,
                    )
                }
            }

            if (source != null) {
                MiniPartDiagramCanvas(
                    placement = p,
                    source = source,
                    modifier = Modifier
                        .size(92.dp)
                        .background(SubtleSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(8.dp)),
                )
            }
        }
    }
}

@Composable
private fun LabelOpRow(
    label: String,
    desc: String,
    dotColor: Color,
    active: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (active) dotColor else Slate300),
        )
        Text(
            text = "$label: $desc",
            style = MaterialTheme.typography.labelSmall,
            color = if (active) Slate900 else Slate400,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MiniPartDiagramCanvas(
    placement: PlacedPart,
    source: CutPart,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val margin = size.minDimension * 0.24f
            val ratio = (placement.width / max(1.0, placement.height)).toFloat()
            var rw = size.width - margin * 2f
            var rh = rw / ratio
            if (rh > size.height - margin * 2f) {
                rh = size.height - margin * 2f
                rw = rh * ratio
            }
            val x = (size.width - rw) / 2f
            val y = (size.height - rh) / 2f
            val rect = RectF(x, y, x + rw, y + rh)

            native.drawRect(rect, Paint().apply { color = AndroidColor.WHITE })
            native.drawRect(
                rect,
                Paint().apply {
                    color = AndroidColor.rgb(71, 85, 105)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                },
            )

            val sides = getPartEdgeSides(source, placement.isRotated)
            val ops = listOf(
                "نوار" to sides.band,
                "شیار" to sides.groove,
                "۴۵°" to sides.farsi,
            ).filter { it.second.isNotEmpty() }

            val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.BLACK
                style = Paint.Style.FILL
            }
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.BLACK
                textSize = max(16f, size.minDimension * 0.12f)
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }

            for ((label, opSides) in ops) {
                for (side in opSides) {
                    val (nx, ny) = when (side) {
                        PartSide.TOP -> 0f to -1f
                        PartSide.BOTTOM -> 0f to 1f
                        PartSide.LEFT -> -1f to 0f
                        PartSide.RIGHT -> 1f to 0f
                    }
                    val (tx, ty) = when (side) {
                        PartSide.TOP, PartSide.BOTTOM -> 1f to 0f
                        PartSide.LEFT, PartSide.RIGHT -> 0f to 1f
                    }
                    val anchor = when (side) {
                        PartSide.TOP -> 0.34f
                        PartSide.BOTTOM -> 0.66f
                        PartSide.LEFT -> 0.32f
                        PartSide.RIGHT -> 0.68f
                    }
                    val horiz = side == PartSide.TOP || side == PartSide.BOTTOM
                    val edgeLen = if (horiz) rw else rh
                    val along = edgeLen * anchor
                    val ax = if (horiz) x + along else if (side == PartSide.LEFT) x else x + rw
                    val ay = if (horiz) (if (side == PartSide.TOP) y else y + rh) else y + along

                    fun pt(out: Float, lat: Float = 0f) = (ax + nx * out + tx * lat) to (ay + ny * out + ty * lat)

                    val gap = 3f
                    val headLen = 7f
                    val arrowLen = 14f
                    val halfShaft = 2.2f
                    val halfHead = 5.5f
                    val arrowPts = listOf(
                        pt(gap, 0f),
                        pt(gap + headLen, -halfHead),
                        pt(gap + headLen, -halfShaft),
                        pt(gap + arrowLen, -halfShaft),
                        pt(gap + arrowLen, halfShaft),
                        pt(gap + headLen, halfShaft),
                        pt(gap + headLen, halfHead),
                    )
                    val path = Path().apply {
                        moveTo(arrowPts[0].first, arrowPts[0].second)
                        for (i in 1 until arrowPts.size) lineTo(arrowPts[i].first, arrowPts[i].second)
                        close()
                    }
                    native.drawPath(path, arrowPaint)

                    val (lx, ly) = pt(gap + arrowLen + 11f)
                    native.save()
                    if (!horiz) native.rotate(-90f, lx, ly)
                    native.drawText(label, lx, ly + 4f, textPaint)
                    native.restore()
                }
            }
        }
    }
}
