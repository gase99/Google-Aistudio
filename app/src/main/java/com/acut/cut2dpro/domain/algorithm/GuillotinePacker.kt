package com.acut.cut2dpro.domain.algorithm

import com.acut.cut2dpro.domain.model.CutPart
import com.acut.cut2dpro.domain.model.normalizeEdgeCount
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

data class StockSheet(
    val length: Double,
    val width: Double,
)

data class LayoutOptions(
    val kerf: Double = 4.0,
    val bandThickness: Double = 0.0,
    val allowRotation: Boolean = true,
)

data class PlacedPart(
    val id: String,
    val name: String,
    val instanceIndex: Int,
    val sheetIndex: Int,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val isRotated: Boolean,
)

data class SheetLayout(
    val sheetIndex: Int,
    val placements: List<PlacedPart>,
    val usedArea: Double,
    val wasteArea: Double,
    val utilizationPercentage: Double,
    val wastePercentage: Double,
)

data class UnplacedPart(
    val id: String,
    val name: String,
    val instanceIndex: Int,
    val length: Double,
    val width: Double,
    val reason: String = "LARGER_THAN_SHEET",
)

data class LayoutSummary(
    val sheetCount: Int,
    val totalParts: Int,
    val placedCount: Int,
    val unplacedCount: Int,
    val stockAreaPerSheet: Double,
    val totalSheetArea: Double,
    val usedArea: Double,
    val wasteArea: Double,
    val utilizationPercentage: Double,
    val wastePercentage: Double,
)

data class LayoutResult(
    val stock: StockSheet,
    val kerf: Double,
    val bandThickness: Double,
    val allowRotation: Boolean,
    val sheets: List<SheetLayout>,
    val placements: List<PlacedPart>,
    val unplaced: List<UnplacedPart>,
    val summary: LayoutSummary,
)

private data class FreeRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

private data class PartInstance(
    val id: String,
    val name: String,
    val instanceIndex: Int,
    val length: Double,
    val width: Double,
    val area: Double,
    val maxDimension: Double,
    val originalOrder: Int,
)

private class WorkingSheet(
    val sheetIndex: Int,
    val freeRects: MutableList<FreeRect>,
    val placements: MutableList<PlacedPart> = mutableListOf(),
    var usedArea: Double = 0.0,
)

private data class PlacementCandidate(
    val sheet: WorkingSheet,
    val freeRectIndex: Int,
    val w: Double,
    val h: Double,
    val isRotated: Boolean,
    val areaLeftover: Double,
    val shortSideLeftover: Double,
    val longSideLeftover: Double,
)

private const val EPS = 1e-9

private fun round2(n: Double): Double = round(n * 100.0) / 100.0

private fun splitFreeRect(
    free: FreeRect,
    partW: Double,
    partH: Double,
    occupiedW: Double,
    occupiedH: Double,
): List<FreeRect> {
    val leftoverW = free.width - occupiedW
    val leftoverH = free.height - occupiedH
    val result = mutableListOf<FreeRect>()

    if (leftoverW >= leftoverH) {
        if (leftoverW > EPS) {
            result.add(
                FreeRect(
                    x = round2(free.x + occupiedW),
                    y = free.y,
                    width = round2(leftoverW),
                    height = free.height,
                )
            )
        }
        if (leftoverH > EPS) {
            result.add(
                FreeRect(
                    x = free.x,
                    y = round2(free.y + occupiedH),
                    width = round2(partW),
                    height = round2(leftoverH),
                )
            )
        }
    } else {
        if (leftoverH > EPS) {
            result.add(
                FreeRect(
                    x = free.x,
                    y = round2(free.y + occupiedH),
                    width = free.width,
                    height = round2(leftoverH),
                )
            )
        }
        if (leftoverW > EPS) {
            result.add(
                FreeRect(
                    x = round2(free.x + occupiedW),
                    y = free.y,
                    width = round2(leftoverW),
                    height = round2(partH),
                )
            )
        }
    }

    return result
}

private fun mergeFreeRects(rects: List<FreeRect>): MutableList<FreeRect> {
    val list = rects.toMutableList()
    var mergedSomething = true

    while (mergedSomething) {
        mergedSomething = false
        outer@ for (i in list.indices) {
            for (j in i + 1 until list.size) {
                val a = list[i]
                val b = list[j]

                if (a.x == b.x && a.width == b.width) {
                    if (round2(a.y + a.height) == b.y || round2(b.y + b.height) == a.y) {
                        val y = min(a.y, b.y)
                        list.removeAt(j)
                        list[i] = FreeRect(
                            x = a.x,
                            y = y,
                            width = a.width,
                            height = round2(a.height + b.height),
                        )
                        mergedSomething = true
                        break@outer
                    }
                }

                if (a.y == b.y && a.height == b.height) {
                    if (round2(a.x + a.width) == b.x || round2(b.x + b.width) == a.x) {
                        val x = min(a.x, b.x)
                        list.removeAt(j)
                        list[i] = FreeRect(
                            x = x,
                            y = a.y,
                            width = round2(a.width + b.width),
                            height = a.height,
                        )
                        mergedSomething = true
                        break@outer
                    }
                }
            }
        }
    }

    return list.filterIndexed { i, r ->
        list.indices.none { j ->
            if (j == i) return@none false
            val other = list[j]
            r.x >= other.x &&
                r.y >= other.y &&
                r.x + r.width <= other.x + other.width + EPS &&
                r.y + r.height <= other.y + other.height + EPS
        }
    }.toMutableList()
}

private data class Orientation(val w: Double, val h: Double, val isRotated: Boolean)

private fun findBestPlacement(
    sheets: List<WorkingSheet>,
    instance: PartInstance,
    allowRotation: Boolean,
): PlacementCandidate? {
    val orientations = buildList {
        add(Orientation(instance.length, instance.width, false))
        if (allowRotation && instance.length != instance.width) {
            add(Orientation(instance.width, instance.length, true))
        }
    }

    var best: PlacementCandidate? = null

    for (sheet in sheets) {
        sheet.freeRects.forEachIndexed { freeRectIndex, free ->
            for (o in orientations) {
                if (o.w > free.width + EPS || o.h > free.height + EPS) continue

                val candidate = PlacementCandidate(
                    sheet = sheet,
                    freeRectIndex = freeRectIndex,
                    w = o.w,
                    h = o.h,
                    isRotated = o.isRotated,
                    areaLeftover = free.width * free.height - o.w * o.h,
                    shortSideLeftover = min(free.width - o.w, free.height - o.h),
                    longSideLeftover = max(free.width - o.w, free.height - o.h),
                )

                val isBetter = best == null ||
                    candidate.areaLeftover < best.areaLeftover - EPS ||
                    (abs(candidate.areaLeftover - best.areaLeftover) <= EPS &&
                        (candidate.shortSideLeftover < best.shortSideLeftover - EPS ||
                            (abs(candidate.shortSideLeftover - best.shortSideLeftover) <= EPS &&
                                (candidate.longSideLeftover < best.longSideLeftover - EPS ||
                                    (abs(candidate.longSideLeftover - best.longSideLeftover) <= EPS &&
                                        !candidate.isRotated &&
                                        best.isRotated)))))

                if (isBetter) {
                    best = candidate
                }
            }
        }
    }

    return best
}

private fun applyPlacement(
    candidate: PlacementCandidate,
    instance: PartInstance,
    kerf: Double,
): PlacedPart {
    val sheet = candidate.sheet
    val free = sheet.freeRects[candidate.freeRectIndex]

    val placed = PlacedPart(
        id = instance.id,
        name = instance.name,
        instanceIndex = instance.instanceIndex,
        sheetIndex = sheet.sheetIndex,
        x = round2(free.x),
        y = round2(free.y),
        width = candidate.w,
        height = candidate.h,
        isRotated = candidate.isRotated,
    )

    val occupiedW = if (candidate.w < free.width) min(free.width, candidate.w + kerf) else free.width
    val occupiedH = if (candidate.h < free.height) min(free.height, candidate.h + kerf) else free.height

    val remainders = splitFreeRect(free, candidate.w, candidate.h, occupiedW, occupiedH)
    sheet.freeRects.removeAt(candidate.freeRectIndex)
    sheet.freeRects.addAll(candidate.freeRectIndex, remainders)
    val merged = mergeFreeRects(sheet.freeRects)
    sheet.freeRects.clear()
    sheet.freeRects.addAll(merged)

    sheet.placements.add(placed)
    sheet.usedArea += candidate.w * candidate.h
    return placed
}

fun calculateLayout(
    parts: List<CutPart>,
    stock: StockSheet,
    options: LayoutOptions,
): LayoutResult {
    val kerf = max(0.0, options.kerf)
    val bandThickness = max(0.0, options.bandThickness)
    val allowRotation = options.allowRotation
    val stockArea = if (stock.length > 0.0 && stock.width > 0.0) stock.length * stock.width else 0.0

    val instances = mutableListOf<PartInstance>()
    var order = 0
    for (part in parts) {
        val qty = floor(part.qty.toDouble()).toInt()
        val cutLength = part.length - normalizeEdgeCount(part.bandWidth) * bandThickness
        val cutWidth = part.width - normalizeEdgeCount(part.bandLength) * bandThickness
        if (cutLength <= 0.0 || cutWidth <= 0.0 || qty <= 0) continue

        for (i in 1..qty) {
            instances.add(
                PartInstance(
                    id = part.id,
                    name = part.name,
                    instanceIndex = i,
                    length = cutLength,
                    width = cutWidth,
                    area = cutLength * cutWidth,
                    maxDimension = max(cutLength, cutWidth),
                    originalOrder = order++,
                )
            )
        }
    }

    instances.sortWith { a, b ->
        b.area.compareTo(a.area).takeIf { it != 0 }
            ?: b.maxDimension.compareTo(a.maxDimension).takeIf { it != 0 }
            ?: a.originalOrder.compareTo(b.originalOrder)
    }

    val sheets = mutableListOf<WorkingSheet>()
    val unplaced = mutableListOf<UnplacedPart>()

    fun openSheet(): WorkingSheet {
        val sheet = WorkingSheet(
            sheetIndex = sheets.size,
            freeRects = mutableListOf(FreeRect(0.0, 0.0, stock.length, stock.width)),
        )
        sheets.add(sheet)
        return sheet
    }

    for (instance in instances) {
        var candidate = findBestPlacement(sheets, instance, allowRotation)

        if (candidate == null && stockArea > 0.0) {
            val freshSheet = openSheet()
            candidate = findBestPlacement(listOf(freshSheet), instance, allowRotation)
            if (candidate == null) {
                sheets.removeAt(sheets.lastIndex)
            }
        }

        if (candidate != null) {
            applyPlacement(candidate, instance, kerf)
        } else {
            unplaced.add(
                UnplacedPart(
                    id = instance.id,
                    name = instance.name,
                    instanceIndex = instance.instanceIndex,
                    length = instance.length,
                    width = instance.width,
                )
            )
        }
    }

    val sheetLayouts = sheets.map { sheet ->
        val wasteArea = max(0.0, stockArea - sheet.usedArea)
        SheetLayout(
            sheetIndex = sheet.sheetIndex,
            placements = sheet.placements.toList(),
            usedArea = round2(sheet.usedArea),
            wasteArea = round2(wasteArea),
            utilizationPercentage = if (stockArea > 0.0) round2((sheet.usedArea / stockArea) * 100.0) else 0.0,
            wastePercentage = if (stockArea > 0.0) round2((wasteArea / stockArea) * 100.0) else 0.0,
        )
    }

    val placements = sheetLayouts.flatMap { it.placements }
    val usedArea = sheets.sumOf { it.usedArea }
    val totalSheetArea = sheets.size * stockArea
    val wasteArea = max(0.0, totalSheetArea - usedArea)

    return LayoutResult(
        stock = stock,
        kerf = kerf,
        bandThickness = bandThickness,
        allowRotation = allowRotation,
        sheets = sheetLayouts,
        placements = placements,
        unplaced = unplaced,
        summary = LayoutSummary(
            sheetCount = sheets.size,
            totalParts = instances.size,
            placedCount = placements.size,
            unplacedCount = unplaced.size,
            stockAreaPerSheet = round2(stockArea),
            totalSheetArea = round2(totalSheetArea),
            usedArea = round2(usedArea),
            wasteArea = round2(wasteArea),
            utilizationPercentage = if (totalSheetArea > 0.0) round2((usedArea / totalSheetArea) * 100.0) else 0.0,
            wastePercentage = if (totalSheetArea > 0.0) round2((wasteArea / totalSheetArea) * 100.0) else 0.0,
        ),
    )
}
