package com.acut.cut2dpro.domain.model

import com.acut.cut2dpro.domain.algorithm.LayoutResult
import com.acut.cut2dpro.domain.algorithm.LayoutSummary
import com.acut.cut2dpro.domain.algorithm.PlacedPart
import com.acut.cut2dpro.domain.algorithm.SheetLayout
import com.acut.cut2dpro.domain.algorithm.StockSheet
import com.acut.cut2dpro.domain.algorithm.UnplacedPart
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class LayoutSettings(
    val stockLength: Double = 3660.0,
    val stockWidth: Double = 1830.0,
    val kerf: Double = 4.0,
    val bandThickness: Double = 0.0,
    val allowRotation: Boolean = true,
)

val DEFAULT_LAYOUT_SETTINGS = LayoutSettings()

const val UNTITLED_PROJECT_NAME = "پروژهٔ بدون نام"

data class StockPreset(
    val label: String,
    val length: Double,
    val width: Double,
)

val STOCK_PRESETS = listOf(
    StockPreset("۳۶۶۰ × ۱۸۳۰", 3660.0, 1830.0),
    StockPreset("۲۴۴۰ × ۱۲۲۰", 2440.0, 1220.0),
    StockPreset("۲۸۰۰ × ۲۰۷۰", 2800.0, 2070.0),
)

data class LayoutState(
    val result: LayoutResult,
    val partsSnapshot: List<CutPart>,
    val signature: String,
)

data class Project(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val parts: List<CutPart>,
    val settings: LayoutSettings,
    val lastLayout: LayoutResult? = null,
    val lastLayoutParts: List<CutPart>? = null,
)

fun createProjectId(): String = UUID.randomUUID().toString()

fun settingsFromLayout(result: LayoutResult): LayoutSettings = LayoutSettings(
    stockLength = result.stock.length,
    stockWidth = result.stock.width,
    kerf = result.kerf,
    bandThickness = result.bandThickness,
    allowRotation = result.allowRotation,
)

fun computeLayoutSignature(parts: List<CutPart>, settings: LayoutSettings): String {
    val root = JSONObject()
    val partsArr = JSONArray()
    for (p in parts) {
        if (!isValidPart(p)) continue
        val tuple = JSONArray()
        tuple.put(p.id)
        tuple.put(p.length)
        tuple.put(p.width)
        tuple.put(p.qty)
        partsArr.put(tuple)
    }
    root.put("parts", partsArr)
    root.put("stockLength", settings.stockLength)
    root.put("stockWidth", settings.stockWidth)
    root.put("kerf", settings.kerf)
    root.put("bandThickness", settings.bandThickness)
    root.put("allowRotation", settings.allowRotation)
    return root.toString()
}

fun computeWorkspaceSignature(
    parts: List<CutPart>,
    settings: LayoutSettings,
    projectName: String,
): String {
    val root = JSONObject()
    root.put("name", projectName)
    root.put("settings", settings.toJson())
    val partsArr = JSONArray()
    for (p in parts) {
        val tuple = JSONArray()
        tuple.put(p.id)
        tuple.put(p.name)
        tuple.put(p.length)
        tuple.put(p.width)
        tuple.put(p.qty)
        tuple.put(p.bandLength)
        tuple.put(p.bandWidth)
        tuple.put(p.farsiLength)
        tuple.put(p.farsiWidth)
        tuple.put(p.grooveLength)
        tuple.put(p.grooveWidth)
        partsArr.put(tuple)
    }
    root.put("parts", partsArr)
    return root.toString()
}

/* ---------------- JSON Serialization Helpers ---------------- */

fun CutPart.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("length", length)
    put("width", width)
    put("qty", qty)
    put("bandLength", bandLength)
    put("bandWidth", bandWidth)
    put("farsiLength", farsiLength)
    put("farsiWidth", farsiWidth)
    put("grooveLength", grooveLength)
    put("grooveWidth", grooveWidth)
}

fun cutPartFromJson(obj: JSONObject): CutPart = CutPart(
    id = obj.optString("id", createPartId()),
    name = obj.optString("name", ""),
    length = obj.optDouble("length", 0.0),
    width = obj.optDouble("width", 0.0),
    qty = obj.optInt("qty", 1),
    bandLength = normalizeEdgeCount(obj.optInt("bandLength", 0)),
    bandWidth = normalizeEdgeCount(obj.optInt("bandWidth", 0)),
    farsiLength = normalizeEdgeCount(obj.optInt("farsiLength", 0)),
    farsiWidth = normalizeEdgeCount(obj.optInt("farsiWidth", 0)),
    grooveLength = normalizeEdgeCount(obj.optInt("grooveLength", 0)),
    grooveWidth = normalizeEdgeCount(obj.optInt("grooveWidth", 0)),
)

fun List<CutPart>.toJsonString(): String {
    val arr = JSONArray()
    forEach { arr.put(it.toJson()) }
    return arr.toString()
}

fun cutPartsFromJsonString(json: String): List<CutPart> {
    val arr = JSONArray(json)
    return List(arr.length()) { idx -> cutPartFromJson(arr.getJSONObject(idx)) }
}

fun LayoutSettings.toJson(): JSONObject = JSONObject().apply {
    put("stockLength", stockLength)
    put("stockWidth", stockWidth)
    put("kerf", kerf)
    put("bandThickness", bandThickness)
    put("allowRotation", allowRotation)
}

fun layoutSettingsFromJson(obj: JSONObject): LayoutSettings = LayoutSettings(
    stockLength = obj.optDouble("stockLength", 3660.0),
    stockWidth = obj.optDouble("stockWidth", 1830.0),
    kerf = obj.optDouble("kerf", 4.0),
    bandThickness = obj.optDouble("bandThickness", 0.0),
    allowRotation = obj.optBoolean("allowRotation", true),
)

fun PlacedPart.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("instanceIndex", instanceIndex)
    put("sheetIndex", sheetIndex)
    put("x", x)
    put("y", y)
    put("width", width)
    put("height", height)
    put("isRotated", isRotated)
}

fun placedPartFromJson(obj: JSONObject): PlacedPart = PlacedPart(
    id = obj.optString("id", ""),
    name = obj.optString("name", ""),
    instanceIndex = obj.optInt("instanceIndex", 1),
    sheetIndex = obj.optInt("sheetIndex", 0),
    x = obj.optDouble("x", 0.0),
    y = obj.optDouble("y", 0.0),
    width = obj.optDouble("width", 0.0),
    height = obj.optDouble("height", 0.0),
    isRotated = obj.optBoolean("isRotated", false),
)

fun LayoutResult.toJson(): JSONObject = JSONObject().apply {
    put("stock", JSONObject().apply {
        put("length", stock.length)
        put("width", stock.width)
    })
    put("kerf", kerf)
    put("bandThickness", bandThickness)
    put("allowRotation", allowRotation)

    val sheetsArr = JSONArray()
    for (sheet in sheets) {
        sheetsArr.put(JSONObject().apply {
            put("sheetIndex", sheet.sheetIndex)
            val pArr = JSONArray()
            sheet.placements.forEach { pArr.put(it.toJson()) }
            put("placements", pArr)
            put("usedArea", sheet.usedArea)
            put("wasteArea", sheet.wasteArea)
            put("utilizationPercentage", sheet.utilizationPercentage)
            put("wastePercentage", sheet.wastePercentage)
        })
    }
    put("sheets", sheetsArr)

    val placementsArr = JSONArray()
    placements.forEach { placementsArr.put(it.toJson()) }
    put("placements", placementsArr)

    val unplacedArr = JSONArray()
    for (u in unplaced) {
        unplacedArr.put(JSONObject().apply {
            put("id", u.id)
            put("name", u.name)
            put("instanceIndex", u.instanceIndex)
            put("length", u.length)
            put("width", u.width)
            put("reason", u.reason)
        })
    }
    put("unplaced", unplacedArr)

    put("summary", JSONObject().apply {
        put("sheetCount", summary.sheetCount)
        put("totalParts", summary.totalParts)
        put("placedCount", summary.placedCount)
        put("unplacedCount", summary.unplacedCount)
        put("stockAreaPerSheet", summary.stockAreaPerSheet)
        put("totalSheetArea", summary.totalSheetArea)
        put("usedArea", summary.usedArea)
        put("wasteArea", summary.wasteArea)
        put("utilizationPercentage", summary.utilizationPercentage)
        put("wastePercentage", summary.wastePercentage)
    })
}

fun layoutResultFromJson(obj: JSONObject): LayoutResult {
    val stockObj = obj.getJSONObject("stock")
    val stock = StockSheet(
        length = stockObj.getDouble("length"),
        width = stockObj.getDouble("width"),
    )

    val sheetsArr = obj.getJSONArray("sheets")
    val sheets = List(sheetsArr.length()) { i ->
        val sObj = sheetsArr.getJSONObject(i)
        val pArr = sObj.getJSONArray("placements")
        SheetLayout(
            sheetIndex = sObj.getInt("sheetIndex"),
            placements = List(pArr.length()) { j -> placedPartFromJson(pArr.getJSONObject(j)) },
            usedArea = sObj.getDouble("usedArea"),
            wasteArea = sObj.getDouble("wasteArea"),
            utilizationPercentage = sObj.getDouble("utilizationPercentage"),
            wastePercentage = sObj.getDouble("wastePercentage"),
        )
    }

    val placementsArr = obj.getJSONArray("placements")
    val placements = List(placementsArr.length()) { i ->
        placedPartFromJson(placementsArr.getJSONObject(i))
    }

    val unplacedArr = obj.optJSONArray("unplaced") ?: JSONArray()
    val unplaced = List(unplacedArr.length()) { i ->
        val uObj = unplacedArr.getJSONObject(i)
        UnplacedPart(
            id = uObj.getString("id"),
            name = uObj.optString("name", ""),
            instanceIndex = uObj.getInt("instanceIndex"),
            length = uObj.getDouble("length"),
            width = uObj.getDouble("width"),
            reason = uObj.optString("reason", "LARGER_THAN_SHEET"),
        )
    }

    val sumObj = obj.getJSONObject("summary")
    val summary = LayoutSummary(
        sheetCount = sumObj.getInt("sheetCount"),
        totalParts = sumObj.getInt("totalParts"),
        placedCount = sumObj.getInt("placedCount"),
        unplacedCount = sumObj.getInt("unplacedCount"),
        stockAreaPerSheet = sumObj.getDouble("stockAreaPerSheet"),
        totalSheetArea = sumObj.getDouble("totalSheetArea"),
        usedArea = sumObj.getDouble("usedArea"),
        wasteArea = sumObj.getDouble("wasteArea"),
        utilizationPercentage = sumObj.getDouble("utilizationPercentage"),
        wastePercentage = sumObj.getDouble("wastePercentage"),
    )

    return LayoutResult(
        stock = stock,
        kerf = obj.getDouble("kerf"),
        bandThickness = obj.optDouble("bandThickness", 0.0),
        allowRotation = obj.getBoolean("allowRotation"),
        sheets = sheets,
        placements = placements,
        unplaced = unplaced,
        summary = summary,
    )
}
