package com.acut.cut2dpro.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acut.cut2dpro.domain.algorithm.EdgeOp
import com.acut.cut2dpro.domain.algorithm.LayoutResult
import com.acut.cut2dpro.domain.algorithm.PlacedPart
import com.acut.cut2dpro.domain.algorithm.SheetLayout
import com.acut.cut2dpro.domain.algorithm.StockSheet
import com.acut.cut2dpro.domain.algorithm.buildFarsiZigzagPoints
import com.acut.cut2dpro.domain.algorithm.computePlacedPartGeometry
import com.acut.cut2dpro.domain.algorithm.getPlacementCode
import com.acut.cut2dpro.domain.model.CutPart
import com.acut.cut2dpro.domain.model.describeOperations
import com.acut.cut2dpro.domain.model.formatDimValue
import com.acut.cut2dpro.domain.model.formatFaDecimal
import com.acut.cut2dpro.domain.model.formatFaInt
import com.acut.cut2dpro.ui.theme.Slate100
import com.acut.cut2dpro.ui.theme.Slate200
import com.acut.cut2dpro.ui.theme.Slate300
import com.acut.cut2dpro.ui.theme.Slate500
import com.acut.cut2dpro.ui.theme.Slate600
import com.acut.cut2dpro.ui.theme.Slate800
import com.acut.cut2dpro.ui.theme.Slate900
import com.acut.cut2dpro.ui.theme.SubtleSurface
import kotlin.math.max
import kotlin.math.min

private data class InspectedPartInfo(
    val placement: PlacedPart,
    val code: String,
    val source: CutPart?,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CuttingMapSection(
    result: LayoutResult,
    parts: List<CutPart>,
    zoomPercent: Int,
    onZoomChange: (Int) -> Unit,
) {
    val partsById = remember(parts) { parts.associateBy { it.id } }
    var inspected by remember { mutableStateOf<InspectedPartInfo?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Legend + Zoom Bar
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "نقشهٔ برش صفحه‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate900,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TouchApp,
                                contentDescription = null,
                                tint = Slate500,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "برای دیدن جزئیات هر قطعه، روی آن لمس کنید",
                                style = MaterialTheme.typography.labelMedium,
                                color = Slate500,
                            )
                        }
                    }

                    // Zoom controls
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SubtleSurface,
                        border = BorderStroke(1.dp, Slate200),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            IconButton(
                                onClick = { onZoomChange(zoomPercent - 25) },
                                enabled = zoomPercent > 50,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("map_zoom_out_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Remove,
                                    contentDescription = "کوچک‌نمایی نقشه",
                                    tint = if (zoomPercent > 50) Slate800 else Slate300,
                                )
                            }
                            TextButton(
                                onClick = { onZoomChange(100) },
                                modifier = Modifier.testTag("map_zoom_reset_button"),
                            ) {
                                Text(
                                    text = "${formatFaInt(zoomPercent)}٪",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Slate900,
                                )
                            }
                            IconButton(
                                onClick = { onZoomChange(zoomPercent + 25) },
                                enabled = zoomPercent < 300,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("map_zoom_in_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "بزرگ‌نمایی نقشه",
                                    tint = if (zoomPercent < 300) Slate800 else Slate300,
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate200)

                // Edge operation B&W legend
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LegendItem(letter = "ن", label = "نوار (لبه‌چسب)", mode = LegendMode.BAND)
                    LegendItem(letter = "ش", label = "شیار (پشت‌بند)", mode = LegendMode.GROOVE)
                    LegendItem(letter = "ف", label = "فارسی ۴۵°", mode = LegendMode.FARSI)
                    LegendItem(letter = null, label = "ضایعات / باقی‌مانده", mode = LegendMode.WASTE)
                }
            }
        }

        // Sheets
        result.sheets.forEach { sheet ->
            SheetMapCard(
                sheet = sheet,
                totalSheets = result.sheets.size,
                stock = result.stock,
                partsById = partsById,
                zoomPercent = zoomPercent,
                onSelectPart = { placement, indexInSheet ->
                    inspected = InspectedPartInfo(
                        placement = placement,
                        code = getPlacementCode(sheet.sheetIndex, indexInSheet),
                        source = partsById[placement.id],
                    )
                },
            )
        }
    }

    inspected?.let { info ->
        PartInspectionDialog(
            info = info,
            onDismiss = { inspected = null },
        )
    }
}

private enum class LegendMode {
    BAND,
    GROOVE,
    FARSI,
    WASTE,
}

@Composable
private fun LegendItem(
    letter: String?,
    label: String,
    mode: LegendMode,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(SubtleSurface, RoundedCornerShape(10.dp))
            .border(1.dp, Slate200, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Canvas(
            modifier = Modifier
                .size(width = 30.dp, height = 16.dp)
                .border(1.dp, Color.Black),
        ) {
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                val rect = RectF(0f, 0f, size.width, size.height)
                native.drawRect(rect, Paint().apply { color = AndroidColor.WHITE })
                when (mode) {
                    LegendMode.BAND -> {
                        native.drawRect(
                            0f,
                            0f,
                            size.width,
                            size.height * 0.36f,
                            Paint().apply { color = AndroidColor.BLACK },
                        )
                    }
                    LegendMode.GROOVE -> {
                        val gRect = RectF(3f, size.height * 0.25f, size.width - 3f, size.height * 0.75f)
                        native.drawRect(
                            gRect,
                            Paint().apply {
                                color = AndroidColor.BLACK
                                style = Paint.Style.STROKE
                                strokeWidth = 2f
                            },
                        )
                        native.drawLine(
                            gRect.left,
                            gRect.centerY(),
                            gRect.right,
                            gRect.centerY(),
                            Paint().apply {
                                color = AndroidColor.BLACK
                                strokeWidth = 2f
                                pathEffect = DashPathEffect(floatArrayOf(5f, 3f), 0f)
                            },
                        )
                    }
                    LegendMode.FARSI -> {
                        val p = Path()
                        var xx = 3f
                        var up = true
                        p.moveTo(xx, size.height * 0.72f)
                        while (xx < size.width - 3f) {
                            xx += 6f
                            p.lineTo(min(xx, size.width - 3f), if (up) size.height * 0.28f else size.height * 0.72f)
                            up = !up
                        }
                        native.drawPath(
                            p,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = AndroidColor.BLACK
                                style = Paint.Style.STROKE
                                strokeWidth = 2.2f
                            },
                        )
                    }
                    LegendMode.WASTE -> {
                        drawHatchOnNativeCanvas(native, rect, 7f)
                    }
                }
            }
        }
        if (letter != null) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.5.dp, Color.Black, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Slate800,
        )
    }
}

@Composable
private fun SheetMapCard(
    sheet: SheetLayout,
    totalSheets: Int,
    stock: StockSheet,
    partsById: Map<String, CutPart>,
    zoomPercent: Int,
    onSelectPart: (PlacedPart, Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, Slate300),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Sheet header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SubtleSurface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "صفحهٔ ${formatFaInt(sheet.sheetIndex + 1)} از ${formatFaInt(totalSheets)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate900,
                )
                Text(
                    text = "بهره‌وری ${formatFaDecimal(sheet.utilizationPercentage)}٪  ·  پرت ${formatFaDecimal(sheet.wastePercentage)}٪  ·  ${formatFaInt(sheet.placements.size)} قطعه",
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate600,
                )
            }
            HorizontalDivider(color = Slate200)

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            ) {
                val baseWidthDp = maxWidth
                val zoomedWidthDp = baseWidthDp * (zoomPercent / 100f)
                val aspect = (stock.width / max(1.0, stock.length)).toFloat()
                val canvasHeightDp = zoomedWidthDp * aspect
                val density = LocalDensity.current
                val canvasWidthPx = with(density) { zoomedWidthDp.toPx() }
                val pxPerMm = (canvasWidthPx / stock.length.toFloat()).coerceAtLeast(0.01f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    Canvas(
                        modifier = Modifier
                            .width(zoomedWidthDp)
                            .height(canvasHeightDp)
                            .pointerInput(sheet.sheetIndex, sheet.placements, pxPerMm) {
                                detectTapGestures { tapOffset: Offset ->
                                    val xMm = tapOffset.x / pxPerMm
                                    val yMm = tapOffset.y / pxPerMm
                                    for (i in sheet.placements.indices.reversed()) {
                                        val p = sheet.placements[i]
                                        if (xMm >= p.x && xMm <= p.x + p.width && yMm >= p.y && yMm <= p.y + p.height) {
                                            onSelectPart(p, i)
                                            break
                                        }
                                    }
                                }
                            }
                            .testTag("cutting_map_sheet_${sheet.sheetIndex}"),
                    ) {
                        val localPxPerMm = (size.width / stock.length.toFloat()).coerceAtLeast(0.01f)
                        drawIntoCanvas { canvas ->
                            val native = canvas.nativeCanvas
                            val sheetRect = RectF(0f, 0f, size.width, size.height)
                            val scale = max(stock.length, stock.width) / 1000.0
                            drawHatchOnNativeCanvas(
                                native,
                                sheetRect,
                                max(6f, (16.0 * scale * localPxPerMm).toFloat()),
                            )
                            native.drawRect(
                                sheetRect,
                                Paint().apply {
                                    color = AndroidColor.BLACK
                                    style = Paint.Style.STROKE
                                    strokeWidth = max(2f, (2.4 * scale * localPxPerMm).toFloat())
                                },
                            )

                            for (placement in sheet.placements) {
                                drawPlacedPartOnNativeCanvas(
                                    canvas = native,
                                    placement = placement,
                                    source = partsById[placement.id],
                                    scale = scale,
                                    pxPerMm = localPxPerMm,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun drawHatchOnNativeCanvas(
    canvas: android.graphics.Canvas,
    rect: RectF,
    stepPx: Float,
) {
    canvas.drawRect(rect, Paint().apply { color = AndroidColor.WHITE; style = Paint.Style.FILL })
    canvas.save()
    canvas.clipRect(rect)
    val hatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(75, 85, 99)
        strokeWidth = 1.4f
    }
    val step = max(6f, stepPx)
    val total = rect.width() + rect.height()
    var offset = -rect.height()
    while (offset < total) {
        canvas.drawLine(
            rect.left + offset,
            rect.bottom,
            rect.left + offset + rect.height(),
            rect.top,
            hatchPaint,
        )
        offset += step
    }
    canvas.restore()
}

private fun drawPlacedPartOnNativeCanvas(
    canvas: android.graphics.Canvas,
    placement: PlacedPart,
    source: CutPart?,
    scale: Double,
    pxPerMm: Float,
) {
    val geo = computePlacedPartGeometry(placement, source, scale)
    val rect = RectF(
        (placement.x * pxPerMm).toFloat(),
        (placement.y * pxPerMm).toFloat(),
        ((placement.x + placement.width) * pxPerMm).toFloat(),
        ((placement.y + placement.height) * pxPerMm).toFloat(),
    )
    canvas.drawRect(rect, Paint().apply { color = AndroidColor.WHITE; style = Paint.Style.FILL })

    val m = geo.metrics
    val inset = m.u * 0.4
    val blackFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.BLACK; style = Paint.Style.FILL }
    val whiteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE; style = Paint.Style.FILL }

    for (item in geo.edges) {
        val f = item.frame
        for (op in item.edge.ops) {
            val (d0, d1) = m.zone(item.edge.ops, op)
            when (op) {
                EdgeOp.BAND -> {
                    val b = f.bandRect(0.0, f.len, d0, d1)
                    canvas.drawRect(
                        (b.x * pxPerMm).toFloat(),
                        (b.y * pxPerMm).toFloat(),
                        ((b.x + b.width) * pxPerMm).toFloat(),
                        ((b.y + b.height) * pxPerMm).toFloat(),
                        blackFill,
                    )
                }
                EdgeOp.GROOVE -> {
                    val b = f.bandRect(inset, f.len - inset, d0, d1)
                    val r = RectF(
                        (b.x * pxPerMm).toFloat(),
                        (b.y * pxPerMm).toFloat(),
                        ((b.x + b.width) * pxPerMm).toFloat(),
                        ((b.y + b.height) * pxPerMm).toFloat(),
                    )
                    canvas.drawRect(r, whiteFill)
                    canvas.drawRect(
                        r,
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = AndroidColor.BLACK
                            style = Paint.Style.STROKE
                            strokeWidth = max(1f, (m.stroke * 1.3 * pxPerMm).toFloat())
                        },
                    )
                    val (cx0, cy0) = f.at(inset, (d0 + d1) / 2.0)
                    val (cx1, cy1) = f.at(f.len - inset, (d0 + d1) / 2.0)
                    canvas.drawLine(
                        (cx0 * pxPerMm).toFloat(),
                        (cy0 * pxPerMm).toFloat(),
                        (cx1 * pxPerMm).toFloat(),
                        (cy1 * pxPerMm).toFloat(),
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = AndroidColor.BLACK
                            strokeWidth = max(1.1f, (m.stroke * 1.6 * pxPerMm).toFloat())
                            pathEffect = DashPathEffect(
                                floatArrayOf(
                                    max(2.5f, (m.u * 1.1 * pxPerMm).toFloat()),
                                    max(1.8f, (m.u * 0.7 * pxPerMm).toFloat()),
                                ),
                                0f,
                            )
                        },
                    )
                }
                EdgeOp.FARSI -> {
                    val pts = buildFarsiZigzagPoints(f, d0, d1, inset)
                    if (pts.size >= 2) {
                        val path = Path()
                        path.moveTo((pts[0].first * pxPerMm).toFloat(), (pts[0].second * pxPerMm).toFloat())
                        for (i in 1 until pts.size) {
                            path.lineTo((pts[i].first * pxPerMm).toFloat(), (pts[i].second * pxPerMm).toFloat())
                        }
                        canvas.drawPath(
                            path,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = AndroidColor.BLACK
                                style = Paint.Style.STROKE
                                strokeWidth = max(1f, (m.stroke * 1.2 * pxPerMm).toFloat())
                                strokeJoin = Paint.Join.MITER
                            },
                        )
                    }
                }
            }
        }
    }

    // Crisp outer cut rectangle
    canvas.drawRect(
        rect,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.BLACK
            style = Paint.Style.STROKE
            strokeWidth = max(1.2f, (1.8 * scale * pxPerMm).toFloat())
        },
    )

    // Edge tags
    for (item in geo.edges) {
        val tag = item.tag ?: continue
        val rPx = (m.tagR * pxPerMm).toFloat()
        val halfWPx = (tag.halfW * pxPerMm).toFloat()
        val cxPx = (tag.cx * pxPerMm).toFloat()
        val cyPx = (tag.cy * pxPerMm).toFloat()
        val pill = RectF(cxPx - halfWPx, cyPx - rPx, cxPx + halfWPx, cyPx + rPx)
        canvas.drawRoundRect(pill, rPx, rPx, whiteFill)
        canvas.drawRoundRect(
            pill,
            rPx,
            rPx,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.BLACK
                style = Paint.Style.STROKE
                strokeWidth = max(1f, (max(m.stroke, m.tagR * 0.12) * pxPerMm).toFloat())
            },
        )
        val letters = item.edge.ops.joinToString(" ") { it.letter }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.BLACK
            textSize = rPx * 1.25f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val fm = textPaint.fontMetrics
        canvas.drawText(letters, cxPx, cyPx - (fm.ascent + fm.descent) / 2f, textPaint)
    }

    // Dimension labels
    for (d in geo.dims) {
        val lx = (d.x * pxPerMm).toFloat()
        val ly = (d.y * pxPerMm).toFloat()
        val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.BLACK
            textSize = (d.fontSize * pxPerMm).toFloat()
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val fm = tPaint.fontMetrics
        canvas.save()
        if (d.rotate) canvas.rotate(-90f, lx, ly)
        canvas.drawText(d.text, lx, ly - (fm.ascent + fm.descent) / 2f, tPaint)
        canvas.restore()
    }

    // Center name or fallback index
    if (geo.nameFit.size >= 7.0) {
        val nx = (geo.nameX * pxPerMm).toFloat()
        val ny = (geo.nameY * pxPerMm).toFloat()
        val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.BLACK
            textSize = (geo.nameFit.size * pxPerMm).toFloat()
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val fm = tPaint.fontMetrics
        canvas.save()
        if (geo.nameFit.vertical) canvas.rotate(-90f, nx, ny)
        canvas.drawText(geo.name, nx, ny - (fm.ascent + fm.descent) / 2f, tPaint)
        canvas.restore()
    } else if (geo.showFallbackIndex) {
        val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.BLACK
            textSize = (min(30.0, min(placement.width, placement.height) / 1.6) * pxPerMm).toFloat()
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val fm = tPaint.fontMetrics
        canvas.drawText(
            placement.instanceIndex.toString(),
            rect.centerX(),
            rect.centerY() - (fm.ascent + fm.descent) / 2f,
            tPaint,
        )
    }
}

@Composable
private fun PartInspectionDialog(
    info: InspectedPartInfo,
    onDismiss: () -> Unit,
) {
    val p = info.placement
    val source = info.source
    val partName = (p.name.ifBlank { source?.name ?: "" }).trim().ifEmpty { "بدون نام" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate900,
                    ) {
                        Text(
                            text = info.code,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Text(
                        text = partName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate900,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "بستن")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("ورق", "صفحهٔ ${formatFaInt(p.sheetIndex + 1)}")
                DetailRow(
                    "نمونه",
                    "${formatFaInt(p.instanceIndex)} از ${formatFaInt(source?.qty ?: p.instanceIndex)}",
                )
                DetailRow(
                    "ابعاد اصلی",
                    "${formatDimValue(source?.length ?: p.width)} × ${formatDimValue(source?.width ?: p.height)} mm",
                )
                DetailRow(
                    "ابعاد برش روی ورق",
                    "${formatDimValue(p.width)} × ${formatDimValue(p.height)} mm" +
                        if (p.isRotated) " (چرخیده ۹۰°)" else "",
                )
                DetailRow(
                    "مختصات (x, y)",
                    "(${formatDimValue(p.x)} , ${formatDimValue(p.y)}) mm",
                )
                HorizontalDivider(color = Slate200)
                Text(
                    text = "عملیات لبه‌ها",
                    style = MaterialTheme.typography.labelLarge,
                    color = Slate800,
                )
                Text(
                    text = describeOperations(source),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600,
                )
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_inspection_dialog_button"),
            ) {
                Text("بستن")
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Slate500)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Slate900,
        )
    }
}
