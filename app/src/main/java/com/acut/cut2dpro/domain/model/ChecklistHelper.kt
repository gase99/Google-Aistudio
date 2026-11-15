package com.acut.cut2dpro.domain.model

import com.acut.cut2dpro.domain.algorithm.LayoutResult
import com.acut.cut2dpro.domain.algorithm.PlacedPart
import com.acut.cut2dpro.domain.algorithm.getPlacementCode

data class ChecklistEntry(
    val placement: PlacedPart,
    val indexInSheet: Int,
    val code: String,
    val source: CutPart?,
)

data class ChecklistGroup(
    val key: String,
    val name: String,
    val dims: String,
    val description: String,
    val count: Int,
    val sheets: List<Int>,
    val codes: List<String>,
)

fun describeEdgeCount(value: Int): String? {
    val count = normalizeEdgeCount(value)
    return when (count) {
        0 -> null
        1 -> "یک لبه"
        else -> "دو لبه"
    }
}

fun describeOperations(part: CutPart?): String {
    if (part == null) return "اطلاعات عملیات موجود نیست"
    val ops = listOf(
        Triple("نوار", "طول", describeEdgeCount(part.bandLength)),
        Triple("نوار", "عرض", describeEdgeCount(part.bandWidth)),
        Triple("فارسی", "طول", describeEdgeCount(part.farsiLength)),
        Triple("فارسی", "عرض", describeEdgeCount(part.farsiWidth)),
        Triple("شیار", "طول", describeEdgeCount(part.grooveLength)),
        Triple("شیار", "عرض", describeEdgeCount(part.grooveWidth)),
    )
    val partsList = ops.filter { it.third != null }.map { "${it.first} ${it.second}: ${it.third}" }
    return if (partsList.isNotEmpty()) partsList.joinToString("، ") else "بدون عملیات"
}

fun describeFlagsLabel(lengthCount: Int, widthCount: Int): String {
    val lc = normalizeEdgeCount(lengthCount)
    val wc = normalizeEdgeCount(widthCount)
    return when {
        lc > 0 && wc > 0 -> "طول (${formatFaInt(lc)}) و عرض (${formatFaInt(wc)})"
        lc > 0 -> "فقط طول (${formatFaInt(lc)})"
        wc > 0 -> "فقط عرض (${formatFaInt(wc)})"
        else -> "ندارد"
    }
}

fun buildChecklistEntries(result: LayoutResult, parts: List<CutPart>): List<ChecklistEntry> {
    val partsById = parts.associateBy { it.id }
    val counters = mutableMapOf<Int, Int>()
    return result.placements.map { placement ->
        val indexInSheet = counters[placement.sheetIndex] ?: 0
        counters[placement.sheetIndex] = indexInSheet + 1
        ChecklistEntry(
            placement = placement,
            indexInSheet = indexInSheet,
            code = getPlacementCode(placement.sheetIndex, indexInSheet),
            source = partsById[placement.id],
        )
    }
}

fun buildChecklistGroups(entries: List<ChecklistEntry>): List<ChecklistGroup> {
    val groups = LinkedHashMap<String, MutableChecklistGroup>()
    for (entry in entries) {
        val p = entry.placement
        val source = entry.source
        val name = (p.name.ifBlank { source?.name ?: "بدون نام" }).trim().ifEmpty { "بدون نام" }
        val dims = if (source != null) {
            "${formatDimValue(source.length)} × ${formatDimValue(source.width)}"
        } else {
            "${formatDimValue(p.width)} × ${formatDimValue(p.height)}"
        }
        val desc = describeOperations(source)
        val key = "$name|$dims|$desc"
        val existing = groups[key]
        if (existing != null) {
            existing.count += 1
            existing.sheets.add(p.sheetIndex)
            existing.codes.add(entry.code)
        } else {
            groups[key] = MutableChecklistGroup(
                key = key,
                name = name,
                dims = dims,
                description = desc,
                count = 1,
                sheets = sortedSetOf(p.sheetIndex),
                codes = mutableListOf(entry.code),
            )
        }
    }
    return groups.values.map {
        ChecklistGroup(
            key = it.key,
            name = it.name,
            dims = it.dims,
            description = it.description,
            count = it.count,
            sheets = it.sheets.toList(),
            codes = it.codes.toList(),
        )
    }
}

private class MutableChecklistGroup(
    val key: String,
    val name: String,
    val dims: String,
    val description: String,
    var count: Int,
    val sheets: MutableSet<Int>,
    val codes: MutableList<String>,
)
