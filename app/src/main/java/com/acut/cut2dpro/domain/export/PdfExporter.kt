package com.acut.cut2dpro.domain.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.acut.cut2dpro.domain.algorithm.EdgeOp
import com.acut.cut2dpro.domain.algorithm.LayoutResult
import com.acut.cut2dpro.domain.algorithm.PartSide
import com.acut.cut2dpro.domain.algorithm.PlacedPart
import com.acut.cut2dpro.domain.algorithm.SheetLayout
import com.acut.cut2dpro.domain.algorithm.StockSheet
import com.acut.cut2dpro.domain.algorithm.buildFarsiZigzagPoints
import com.acut.cut2dpro.domain.algorithm.computePlacedPartGeometry
import com.acut.cut2dpro.domain.algorithm.getPartEdgeSides
import com.acut.cut2dpro.domain.model.ChecklistEntry
import com.acut.cut2dpro.domain.model.CutPart
import com.acut.cut2dpro.domain.model.buildChecklistEntries
import com.acut.cut2dpro.domain.model.buildChecklistGroups
import com.acut.cut2dpro.domain.model.calculateMaterialSummary
import com.acut.cut2dpro.domain.model.describeFlagsLabel
import com.acut.cut2dpro.domain.model.formatDimValue
import com.acut.cut2dpro.domain.model.formatFaDecimal
import com.acut.cut2dpro.domain.model.formatFaInt
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object PdfExporter {
    // 2x A4 points (1190 x 1684) for super sharp vector text & hairlines
    private const val PAGE_W = 1190
    private const val PAGE_H = 1684
    private const val MARGIN = 36f

    fun mapPdfFileName(result: LayoutResult): String =
        "ACut_map_${formatDimValue(result.stock.length)}x${formatDimValue(result.stock.width)}_${result.summary.sheetCount}sheets.pdf"

    fun labelsPdfFileName(result: LayoutResult): String =
        "ACut_labels_${formatDimValue(result.stock.length)}x${formatDimValue(result.stock.width)}.pdf"

    fun generateMapPdf(context: Context, result: LayoutResult, parts: List<CutPart>): File {
        val doc = PdfDocument()
        val partsById = parts.associateBy { it.id }
        val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        var pageNum = 1

        // 1) One A4 page per stock sheet
        result.sheets.forEachIndexed { idx, sheet ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum++).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            var y = MARGIN
            if (idx == 0) {
                y = drawMapHeader(canvas, result, dateStr, y)
                y += 12f
                y = drawEdgeLegendBox(canvas, y)
                y += 16f
            }

            drawSheetBlock(
                canvas = canvas,
                sheet = sheet,
                totalSheets = result.sheets.size,
                stock = result.stock,
                partsById = partsById,
                topY = y,
                maxBottomY = PAGE_H - MARGIN,
            )

            doc.finishPage(page)
        }

        // 2) Grouped Checklist + Material Invoice on final page(s)
        val entries = buildChecklistEntries(result, parts)
        val groups = buildChecklistGroups(entries)
        val material = calculateMaterialSummary(parts)

        var groupIdx = 0
        var isFirstPartsPage = true
        while (groupIdx < groups.size || isFirstPartsPage) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum++).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            var y = MARGIN

            if (isFirstPartsPage) {
                y = drawChecklistSectionHeader(canvas, result, groups.size, y)
                y += 12f
                isFirstPartsPage = false
            }

            y = drawChecklistTableHeader(canvas, y)
            while (groupIdx < groups.size && y + 54f < PAGE_H - MARGIN - 16f) {
                y = drawChecklistGroupRow(canvas, groupIdx + 1, groups[groupIdx], y)
                groupIdx++
            }

            if (groupIdx >= groups.size) {
                if (y + 150f > PAGE_H - MARGIN) {
                    doc.finishPage(page)
                    val extraPage = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum++).create())
                    extraPage.canvas.drawColor(Color.WHITE)
                    drawMaterialInvoice(extraPage.canvas, material, MARGIN)
                    doc.finishPage(extraPage)
                    break
                } else {
                    y += 20f
                    drawMaterialInvoice(canvas, material, y)
                }
            }
            doc.finishPage(page)
            if (groupIdx >= groups.size) break
        }

        val outFile = File(context.cacheDir, mapPdfFileName(result))
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    fun generateLabelsPdf(context: Context, result: LayoutResult, parts: List<CutPart>): File {
        val doc = PdfDocument()
        val entries = buildChecklistEntries(result, parts)
        val perPage = 15 // 3 columns x 5 rows
        val pages = if (entries.isEmpty()) listOf(emptyList()) else entries.chunked(perPage)

        val pad = 44f
        val gap = 16f
        val cellW = (PAGE_W - pad * 2f - gap * 2f) / 3f
        val cellH = (PAGE_H - pad * 2f - gap * 4f) / 5f

        pages.forEachIndexed { pageIndex, pageEntries ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageIndex + 1).create())
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            for (slot in 0 until perPage) {
                val row = slot / 3
                val col = slot % 3
                // RTL right-to-left visual column order: col 0 on right
                val left = PAGE_W - pad - (col + 1) * cellW - col * gap
                val top = pad + row * (cellH + gap)
                val rect = RectF(left, top, left + cellW, top + cellH)
                if (slot < pageEntries.size) {
                    drawSingleLabel(canvas, rect, pageEntries[slot])
                } else {
                    val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(203, 213, 225)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f
                        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
                    }
                    canvas.drawRoundRect(rect, 10f, 10f, dashPaint)
                }
            }
            doc.finishPage(page)
        }

        val outFile = File(context.cacheDir, labelsPdfFileName(result))
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    /* ---------------- Map PDF Sections ---------------- */

    private fun drawMapHeader(canvas: Canvas, result: LayoutResult, dateStr: String, topY: Float): Float {
        val rightX = PAGE_W - MARGIN
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 18f
            textAlign = Paint.Align.RIGHT
        }
        val leftMetaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        canvas.drawText("نقشهٔ برش A-Cut 2Dpro", rightX, topY + 26f, titlePaint)
        canvas.drawText("تاریخ: $dateStr", rightX, topY + 52f, metaPaint)

        val line1 = "ورق خام: ${formatDimValue(result.stock.length)}×${formatDimValue(result.stock.width)} mm   |   تیغه: ${formatDimValue(result.kerf)} mm   |   نوار: ${formatDimValue(result.bandThickness)} mm"
        val line2 = "صفحات: ${formatFaInt(result.summary.sheetCount)}   |   بهره‌وری: ${formatFaDecimal(result.summary.utilizationPercentage)}٪   |   پرت: ${formatFaDecimal(result.summary.wastePercentage)}٪   |   قطعات: ${formatFaInt(result.summary.placedCount)} از ${formatFaInt(result.summary.totalParts)}"
        canvas.drawText(line1, MARGIN, topY + 24f, leftMetaPaint)
        canvas.drawText(line2, MARGIN, topY + 50f, leftMetaPaint)

        val borderY = topY + 64f
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 3f
        }
        canvas.drawLine(MARGIN, borderY, PAGE_W - MARGIN, borderY, linePaint)
        return borderY
    }

    private fun drawEdgeLegendBox(canvas: Canvas, topY: Float): Float {
        val boxH = 74f
        val boxRect = RectF(MARGIN, topY, PAGE_W - MARGIN, topY + boxH)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRect(boxRect, borderPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("راهنمای علائم لبه:", PAGE_W - MARGIN - 14f, topY + 24f, titlePaint)

        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val items = listOf(
            Triple("ن", "نوار (لبه‌چسب)", EdgeOp.BAND),
            Triple("ش", "شیار (پشت‌بند)", EdgeOp.GROOVE),
            Triple("ف", "فارسی ۴۵°", EdgeOp.FARSI),
        )
        var cursorX = PAGE_W - MARGIN - 18f
        val rowY = topY + 48f
        for ((letter, text, op) in items) {
            // Swatch
            val swW = 36f
            val swH = 18f
            val swRect = RectF(cursorX - swW, rowY - swH / 2f, cursorX, rowY + swH / 2f)
            canvas.drawRect(swRect, Paint().apply { color = Color.WHITE })
            canvas.drawRect(swRect, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f })
            when (op) {
                EdgeOp.BAND -> canvas.drawRect(swRect.left, swRect.top, swRect.right, swRect.top + 6f, Paint().apply { color = Color.BLACK })
                EdgeOp.GROOVE -> {
                    val gRect = RectF(swRect.left + 2f, swRect.top + 6f, swRect.right - 2f, swRect.bottom - 5f)
                    canvas.drawRect(gRect, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f })
                    canvas.drawLine(gRect.left, gRect.centerY(), gRect.right, gRect.centerY(), Paint().apply {
                        color = Color.BLACK; strokeWidth = 1.5f; pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
                    })
                }
                EdgeOp.FARSI -> {
                    val p = Path()
                    var xx = swRect.left + 2f
                    var up = true
                    p.moveTo(xx, swRect.top + 10f)
                    while (xx < swRect.right - 2f) {
                        xx += 5f
                        p.lineTo(min(xx, swRect.right - 2f), if (up) swRect.top + 3f else swRect.top + 10f)
                        up = !up
                    }
                    canvas.drawPath(p, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.8f })
                }
            }
            cursorX -= swW + 8f
            // Chip
            val chipR = 11f
            canvas.drawCircle(cursorX - chipR, rowY, chipR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
            canvas.drawCircle(cursorX - chipR, rowY, chipR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.8f })
            val chipText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
            }
            canvas.drawText(letter, cursorX - chipR, rowY + 5f, chipText)
            cursorX -= chipR * 2f + 8f
            canvas.drawText(text, cursorX, rowY + 6f, labelPaint)
            cursorX -= labelPaint.measureText(text) + 34f
        }

        // Waste swatch
        val swRect = RectF(cursorX - 36f, rowY - 9f, cursorX, rowY + 9f)
        drawHatchedRect(canvas, swRect, 6f)
        canvas.drawRect(swRect, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f })
        cursorX -= 44f
        canvas.drawText("ضایعات / باقی‌مانده", cursorX, rowY + 6f, labelPaint)

        return topY + boxH
    }

    private fun drawSheetBlock(
        canvas: Canvas,
        sheet: SheetLayout,
        totalSheets: Int,
        stock: StockSheet,
        partsById: Map<String, CutPart>,
        topY: Float,
        maxBottomY: Float,
    ) {
        val headerH = 42f
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val boxW = PAGE_W - MARGIN * 2f
        val availMapH = maxBottomY - (topY + headerH + 16f)

        val aspect = stock.length / max(1.0, stock.width)
        var mapW = boxW - 20f
        var mapH = (mapW / aspect).toFloat()
        if (mapH > availMapH) {
            mapH = availMapH
            mapW = (mapH * aspect).toFloat()
        }

        val frameBottom = topY + headerH + mapH + 20f
        canvas.drawRect(MARGIN, topY, PAGE_W - MARGIN, frameBottom, borderPaint)
        canvas.drawLine(MARGIN, topY + headerH, PAGE_W - MARGIN, topY + headerH, borderPaint)

        val rightTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.RIGHT
        }
        val leftTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(
            "صفحهٔ ${formatFaInt(sheet.sheetIndex + 1)} از ${formatFaInt(totalSheets)}  (${formatDimValue(stock.length)} × ${formatDimValue(stock.width)} mm)",
            PAGE_W - MARGIN - 14f,
            topY + 28f,
            rightTextPaint,
        )
        canvas.drawText(
            "بهره‌وری ${formatFaDecimal(sheet.utilizationPercentage)}٪  ·  پرت ${formatFaDecimal(sheet.wastePercentage)}٪  ·  ${formatFaInt(sheet.placements.size)} قطعه",
            MARGIN + 14f,
            topY + 28f,
            leftTextPaint,
        )

        val mapLeft = MARGIN + (boxW - mapW) / 2f
        val mapTop = topY + headerH + 10f
        val pxPerMm = mapW / stock.length.toFloat()

        canvas.save()
        canvas.translate(mapLeft, mapTop)
        drawCuttingSheetOnNativeCanvas(canvas, sheet, stock, partsById, pxPerMm)
        canvas.restore()
    }

    private fun drawCuttingSheetOnNativeCanvas(
        canvas: Canvas,
        sheet: SheetLayout,
        stock: StockSheet,
        partsById: Map<String, CutPart>,
        pxPerMm: Float,
    ) {
        val wPx = (stock.length * pxPerMm).toFloat()
        val hPx = (stock.width * pxPerMm).toFloat()
        val sheetRect = RectF(0f, 0f, wPx, hPx)

        val scale = max(stock.length, stock.width) / 1000.0
        drawHatchedRect(canvas, sheetRect, max(5f, (16.0 * scale * pxPerMm).toFloat()))
        canvas.drawRect(sheetRect, Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = max(1.5f, (2.4 * scale * pxPerMm).toFloat())
        })

        for (placement in sheet.placements) {
            drawPlacedPartNative(canvas, placement, partsById[placement.id], scale, pxPerMm)
        }
    }

    private fun drawPlacedPartNative(
        canvas: Canvas,
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
        canvas.drawRect(rect, Paint().apply { color = Color.WHITE; style = Paint.Style.FILL })

        val m = geo.metrics
        val inset = m.u * 0.4
        val blackFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
        val whiteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }

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
                        canvas.drawRect(r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.BLACK; style = Paint.Style.STROKE
                            strokeWidth = max(0.8f, (m.stroke * 1.3 * pxPerMm).toFloat())
                        })
                        val (cx0, cy0) = f.at(inset, (d0 + d1) / 2.0)
                        val (cx1, cy1) = f.at(f.len - inset, (d0 + d1) / 2.0)
                        canvas.drawLine(
                            (cx0 * pxPerMm).toFloat(),
                            (cy0 * pxPerMm).toFloat(),
                            (cx1 * pxPerMm).toFloat(),
                            (cy1 * pxPerMm).toFloat(),
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.BLACK
                                strokeWidth = max(0.9f, (m.stroke * 1.6 * pxPerMm).toFloat())
                                pathEffect = DashPathEffect(
                                    floatArrayOf(max(2f, (m.u * 1.1 * pxPerMm).toFloat()), max(1.5f, (m.u * 0.7 * pxPerMm).toFloat())),
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
                            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.BLACK
                                style = Paint.Style.STROKE
                                strokeWidth = max(0.8f, (m.stroke * 1.2 * pxPerMm).toFloat())
                                strokeJoin = Paint.Join.MITER
                            })
                        }
                    }
                }
            }
        }

        // Redraw border cleanly on top of edge marks
        canvas.drawRect(rect, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = max(1f, (1.8 * scale * pxPerMm).toFloat())
        })

        // Tags
        for (item in geo.edges) {
            val tag = item.tag ?: continue
            val rPx = (m.tagR * pxPerMm).toFloat()
            val halfWPx = (tag.halfW * pxPerMm).toFloat()
            val cxPx = (tag.cx * pxPerMm).toFloat()
            val cyPx = (tag.cy * pxPerMm).toFloat()
            val pill = RectF(cxPx - halfWPx, cyPx - rPx, cxPx + halfWPx, cyPx + rPx)
            canvas.drawRoundRect(pill, rPx, rPx, whiteFill)
            canvas.drawRoundRect(pill, rPx, rPx, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK; style = Paint.Style.STROKE
                strokeWidth = max(0.8f, (max(m.stroke, m.tagR * 0.12) * pxPerMm).toFloat())
            })
            val letters = item.edge.ops.joinToString(" ") { it.letter }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
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
                color = Color.BLACK
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

        // Center name
        if (geo.nameFit.size >= 7.0) {
            val nx = (geo.nameX * pxPerMm).toFloat()
            val ny = (geo.nameY * pxPerMm).toFloat()
            val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
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
                color = Color.BLACK
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

    private fun drawHatchedRect(canvas: Canvas, rect: RectF, stepPx: Float) {
        canvas.drawRect(rect, Paint().apply { color = Color.WHITE; style = Paint.Style.FILL })
        canvas.save()
        canvas.clipRect(rect)
        val hatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(60, 60, 60)
            strokeWidth = 1.2f
        }
        val step = max(5f, stepPx)
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

    /* ---------------- Checklist & Invoice ---------------- */

    private fun drawChecklistSectionHeader(
        canvas: Canvas,
        result: LayoutResult,
        groupCount: Int,
        topY: Float,
    ): Float {
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.RIGHT
        }
        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("فهرست کنترل تولید — گروه‌بندی‌شده", PAGE_W - MARGIN, topY + 26f, titlePaint)
        canvas.drawText(
            "${formatFaInt(result.summary.placedCount)} قطعه · ${formatFaInt(groupCount)} گروه · ${formatFaInt(result.summary.sheetCount)} ورق",
            MARGIN,
            topY + 26f,
            badgePaint,
        )
        canvas.drawLine(MARGIN, topY + 38f, PAGE_W - MARGIN, topY + 38f, Paint().apply {
            color = Color.BLACK; strokeWidth = 2.5f
        })
        return topY + 42f
    }

    // RTL columns from right to left: row#, name, dims, count, sheets, codes, operations, check
    private val colWidths = floatArrayOf(54f, 160f, 128f, 64f, 76f, 160f, 396f, 80f)
    private val colTitles = arrayOf("ردیف", "نام قطعه", "ابعاد (mm)", "تعداد", "ورق‌ها", "کدهای نقشه", "شرح عملیات", "کنترل")

    private fun drawChecklistTableHeader(canvas: Canvas, topY: Float): Float {
        val rowH = 42f
        val bgPaint = Paint().apply { color = Color.rgb(15, 23, 42); style = Paint.Style.FILL }
        canvas.drawRect(MARGIN, topY, PAGE_W - MARGIN, topY + rowH, bgPaint)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val borderPaint = Paint().apply { color = Color.rgb(51, 65, 85); strokeWidth = 1f }
        var right = PAGE_W - MARGIN
        for (i in colWidths.indices) {
            val left = right - colWidths[i]
            canvas.drawText(colTitles[i], (left + right) / 2f, topY + 27f, textPaint)
            canvas.drawLine(left, topY, left, topY + rowH, borderPaint)
            right = left
        }
        return topY + rowH
    }

    private fun drawChecklistGroupRow(
        canvas: Canvas,
        index: Int,
        group: com.acut.cut2dpro.domain.model.ChecklistGroup,
        topY: Float,
    ): Float {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 15f
        }
        val opsLayout = StaticLayout.Builder.obtain(
            group.description, 0, group.description.length, textPaint, (colWidths[6] - 16f).toInt().coerceAtLeast(40)
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        val codesText = group.codes.joinToString("، ")
        val codesLayout = StaticLayout.Builder.obtain(
            codesText, 0, codesText.length, textPaint, (colWidths[5] - 12f).toInt().coerceAtLeast(40)
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

        val rowH = max(46f, max(opsLayout.height, codesLayout.height) + 18f)
        val bgColor = if (index % 2 == 1) Color.WHITE else Color.rgb(248, 250, 252)
        canvas.drawRect(MARGIN, topY, PAGE_W - MARGIN, topY + rowH, Paint().apply { color = bgColor })

        val borderPaint = Paint().apply { color = Color.rgb(203, 213, 225); style = Paint.Style.STROKE; strokeWidth = 1f }
        val centerPaint = TextPaint(textPaint).apply { textAlign = Paint.Align.CENTER }
        val rightPaint = TextPaint(textPaint).apply { textAlign = Paint.Align.RIGHT; typeface = Typeface.DEFAULT_BOLD }

        var right = PAGE_W - MARGIN
        val values = arrayOf(
            formatFaInt(index),
            group.name,
            group.dims,
            formatFaInt(group.count),
            group.sheets.joinToString("، ") { formatFaInt(it + 1) },
            codesText,
            group.description,
            "",
        )

        for (i in colWidths.indices) {
            val left = right - colWidths[i]
            val cellRect = RectF(left, topY, right, topY + rowH)
            canvas.drawRect(cellRect, borderPaint)
            val cy = topY + rowH / 2f + 5f
            when (i) {
                1 -> canvas.drawText(values[i].take(22), right - 8f, cy, rightPaint)
                5 -> {
                    canvas.save()
                    canvas.translate(left + 6f, topY + (rowH - codesLayout.height) / 2f)
                    codesLayout.draw(canvas)
                    canvas.restore()
                }
                6 -> {
                    canvas.save()
                    canvas.translate(left + 8f, topY + (rowH - opsLayout.height) / 2f)
                    opsLayout.draw(canvas)
                    canvas.restore()
                }
                7 -> {
                    val box = 24f
                    canvas.drawRect(
                        cellRect.centerX() - box / 2f,
                        cellRect.centerY() - box / 2f,
                        cellRect.centerX() + box / 2f,
                        cellRect.centerY() + box / 2f,
                        Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f },
                    )
                }
                else -> canvas.drawText(values[i], cellRect.centerX(), cy, centerPaint)
            }
            right = left
        }
        return topY + rowH
    }

    private fun drawMaterialInvoice(
        canvas: Canvas,
        material: com.acut.cut2dpro.domain.model.MaterialSummary,
        topY: Float,
    ) {
        val boxH = 126f
        val rect = RectF(MARGIN, topY, PAGE_W - MARGIN, topY + boxH)
        canvas.drawRoundRect(rect, 12f, 12f, Paint().apply { color = Color.rgb(248, 250, 252) })
        canvas.drawRoundRect(rect, 12f, 12f, Paint().apply {
            color = Color.rgb(15, 23, 42); style = Paint.Style.STROKE; strokeWidth = 2.5f
        })

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("فاکتور متریال مصرفی (تخمینی)", PAGE_W - MARGIN - 16f, topY + 28f, titlePaint)

        val cardGap = 16f
        val cardW = (rect.width() - 32f - cardGap * 2f) / 3f
        val cards = listOf(
            "نوار لبه" to "${formatFaDecimal(material.bandMeters)} m",
            "لبهٔ فارسی ۴۵°" to "${formatFaDecimal(material.farsiMeters)} m",
            "شیار" to "${formatFaDecimal(material.grooveMeters)} m",
        )
        cards.forEachIndexed { idx, (label, valText) ->
            val left = rect.right - 16f - (idx + 1) * cardW - idx * cardGap
            val cRect = RectF(left, topY + 42f, left + cardW, topY + boxH - 14f)
            canvas.drawRoundRect(cRect, 8f, 8f, Paint().apply { color = Color.WHITE })
            canvas.drawRoundRect(cRect, 8f, 8f, Paint().apply {
                color = Color.rgb(203, 213, 225); style = Paint.Style.STROKE; strokeWidth = 1.5f
            })
            val lPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105); textSize = 15f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
            }
            val vPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK; textSize = 24f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
            }
            canvas.drawText(label, cRect.centerX(), cRect.top + 24f, lPaint)
            canvas.drawText(valText, cRect.centerX(), cRect.top + 56f, vPaint)
        }
    }

    /* ---------------- Label PDF Drawing ---------------- */

    private fun drawSingleLabel(canvas: Canvas, rect: RectF, entry: ChecklistEntry) {
        val p = entry.placement
        val source = entry.source
        val name = (p.name.ifBlank { source?.name ?: "" }).trim().ifEmpty { "بدون نام" }
        val totalQty = source?.qty ?: p.instanceIndex

        canvas.drawRoundRect(rect, 10f, 10f, Paint().apply { color = Color.WHITE })
        canvas.drawRoundRect(rect, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            pathEffect = DashPathEffect(floatArrayOf(8f, 5f), 0f)
        })

        val pad = 12f
        val diagramW = 104f
        val diagramRect = RectF(rect.left + pad, rect.top + 38f, rect.left + pad + diagramW, rect.bottom - pad)
        if (source != null) {
            drawMiniPartDiagramNative(canvas, diagramRect, p, source)
        }

        val textRight = rect.right - pad
        // Code badge
        val badgeW = 64f
        val badgeH = 24f
        val badgeRect = RectF(textRight - badgeW, rect.top + pad, textRight, rect.top + pad + badgeH)
        canvas.drawRoundRect(badgeRect, 5f, 5f, Paint().apply { color = Color.rgb(30, 41, 59) })
        canvas.drawText(
            entry.code,
            badgeRect.centerX(),
            badgeRect.centerY() + 5f,
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; textSize = 14f; typeface = Typeface.MONOSPACE; isFakeBoldText = true; textAlign = Paint.Align.CENTER
            },
        )

        // Name
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(name.take(18), badgeRect.left - 8f, rect.top + pad + 18f, namePaint)

        // Dimensions
        val dimPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.RIGHT
        }
        val origDims = "${formatDimValue(source?.length ?: p.width)} × ${formatDimValue(source?.width ?: p.height)} mm"
        canvas.drawText("ابعاد: $origDims", textRight, rect.top + pad + 48f, dimPaint)

        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 12.5f
            textAlign = Paint.Align.RIGHT
        }
        val rotStr = if (p.isRotated) " (چرخیده ${formatDimValue(p.width)}×${formatDimValue(p.height)})" else ""
        canvas.drawText(
            "ورق ${formatFaInt(p.sheetIndex + 1)} · نمونه ${formatFaInt(p.instanceIndex)}/${formatFaInt(totalQty)}$rotStr",
            textRight,
            rect.top + pad + 70f,
            metaPaint,
        )
        canvas.drawText(
            "x:${formatDimValue(p.x)}  y:${formatDimValue(p.y)}",
            textRight,
            rect.top + pad + 90f,
            metaPaint,
        )

        if (source != null) {
            val ops = listOf(
                Triple("نوار", describeFlagsLabel(source.bandLength, source.bandWidth), Color.rgb(13, 148, 136) to (source.bandLength + source.bandWidth > 0)),
                Triple("فارسی ۴۵°", describeFlagsLabel(source.farsiLength, source.farsiWidth), Color.rgb(220, 38, 38) to (source.farsiLength + source.farsiWidth > 0)),
                Triple("شیار", describeFlagsLabel(source.grooveLength, source.grooveWidth), Color.rgb(217, 119, 6) to (source.grooveLength + source.grooveWidth > 0)),
            )
            var oy = rect.top + pad + 118f
            for ((label, desc, stylePair) in ops) {
                val (dotColor, active) = stylePair
                canvas.drawCircle(textRight - 5f, oy - 4f, 4.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (active) dotColor else Color.rgb(203, 213, 225)
                })
                val opPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (active) Color.rgb(15, 23, 42) else Color.rgb(148, 163, 184)
                    textSize = 12.5f
                    typeface = if (active) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("$label: $desc", textRight - 14f, oy, opPaint)
                oy += 22f
            }
        }
    }

    private fun drawMiniPartDiagramNative(
        canvas: Canvas,
        bounds: RectF,
        placement: PlacedPart,
        source: CutPart,
    ) {
        val margin = 22f
        val ratio = (placement.width / max(1.0, placement.height)).toFloat()
        var rw = bounds.width() - margin * 2f
        var rh = rw / ratio
        if (rh > bounds.height() - margin * 2f) {
            rh = bounds.height() - margin * 2f
            rw = rh * ratio
        }
        val x = bounds.centerX() - rw / 2f
        val y = bounds.centerY() - rh / 2f
        val partRect = RectF(x, y, x + rw, y + rh)

        canvas.drawRect(partRect, Paint().apply { color = Color.rgb(248, 250, 252) })
        canvas.drawRect(partRect, Paint().apply { color = Color.rgb(100, 116, 139); style = Paint.Style.STROKE; strokeWidth = 1.6f })

        val sides = getPartEdgeSides(source, placement.isRotated)
        val ops = listOf(
            "نوار" to sides.band,
            "شیار" to sides.groove,
            "۴۵°" to sides.farsi,
        ).filter { it.second.isNotEmpty() }

        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
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

                val gap = 2f
                val headLen = 5f
                val arrowLen = 10f
                val halfShaft = 1.8f
                val halfHead = 4.5f
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
                canvas.drawPath(path, arrowPaint)

                val (lx, ly) = pt(gap + arrowLen + 8f)
                canvas.save()
                if (!horiz) canvas.rotate(-90f, lx, ly)
                canvas.drawText(label, lx, ly + 3.5f, textPaint)
                canvas.restore()
            }
        }
    }

    /* ---------------- Delivery Helpers (Share / SAF / Print) ---------------- */

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun writeFileToUri(context: Context, sourceFile: File, targetUri: Uri) {
        context.contentResolver.openOutputStream(targetUri)?.use { out ->
            FileInputStream(sourceFile).use { input ->
                input.copyTo(out)
            }
        }
    }

    fun writeTextToUri(context: Context, content: String, targetUri: Uri) {
        context.contentResolver.openOutputStream(targetUri)?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
        }
    }

    fun writeTextToCacheFile(context: Context, filename: String, content: String): File {
        val file = File(context.cacheDir, filename)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?,
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?,
            ) {
                val fd = destination?.fileDescriptor
                if (fd == null) {
                    callback?.onWriteFailed("فایل خروجی چاپ نامعتبر است")
                    return
                }
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(fd).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        val attrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .build()
        printManager.print(jobName, adapter, attrs)
    }
}
