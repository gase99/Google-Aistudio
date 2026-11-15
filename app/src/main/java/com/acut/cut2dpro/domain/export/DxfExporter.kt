package com.acut.cut2dpro.domain.export

import com.acut.cut2dpro.domain.algorithm.LayoutResult
import com.acut.cut2dpro.domain.algorithm.PartSide
import com.acut.cut2dpro.domain.algorithm.GROOVE_INSET_MM
import com.acut.cut2dpro.domain.algorithm.getPartEdgeSides
import com.acut.cut2dpro.domain.algorithm.getPlacementCode
import com.acut.cut2dpro.domain.model.CutPart
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

const val DXF_SHEET_GAP_MM = 100.0

private fun fmt(n: Double): String {
    val v = round(n * 1000.0) / 1000.0
    val clean = if (v == -0.0) 0.0 else v
    return if (clean == clean.toLong().toDouble()) {
        clean.toLong().toString()
    } else {
        clean.toString()
    }
}

private class DxfBuilder {
    private val lines = mutableListOf<String>()

    fun pair(code: Int, value: String): DxfBuilder {
        lines.add(code.toString())
        lines.add(value)
        return this
    }

    fun pair(code: Int, value: Double): DxfBuilder = pair(code, fmt(value))
    fun pair(code: Int, value: Int): DxfBuilder = pair(code, value.toString())

    fun section(name: String, body: () -> Unit): DxfBuilder {
        pair(0, "SECTION").pair(2, name)
        body()
        pair(0, "ENDSEC")
        return this
    }

    fun closedPolyline(layer: String, points: List<Pair<Double, Double>>): DxfBuilder {
        pair(0, "POLYLINE").pair(8, layer).pair(66, 1).pair(70, 1)
        for ((x, y) in points) {
            pair(0, "VERTEX").pair(8, layer).pair(10, x).pair(20, y).pair(30, 0.0)
        }
        pair(0, "SEQEND").pair(8, layer)
        return this
    }

    fun line(layer: String, x1: Double, y1: Double, x2: Double, y2: Double): DxfBuilder {
        pair(0, "LINE")
            .pair(8, layer)
            .pair(10, x1).pair(20, y1).pair(30, 0.0)
            .pair(11, x2).pair(21, y2).pair(31, 0.0)
        return this
    }

    fun text(layer: String, x: Double, y: Double, height: Double, value: String, rotation: Double = 0.0): DxfBuilder {
        pair(0, "TEXT")
            .pair(8, layer)
            .pair(10, x).pair(20, y).pair(30, 0.0)
            .pair(40, height)
            .pair(1, value)
            .pair(50, rotation)
            .pair(72, 1)
            .pair(11, x).pair(21, y).pair(31, 0.0)
            .pair(73, 2)
        return this
    }

    fun build(): String = lines.joinToString("\n") + "\n"
}

private val DXF_LAYERS = listOf(
    "SHEET" to 8,
    "CUT" to 7,
    "GROOVE" to 30,
    "FARSI_45" to 1,
    "LABEL" to 5,
)

fun generateDxf(result: LayoutResult, parts: List<CutPart>): String {
    val stock = result.stock
    val partsById = parts.associateBy { it.id }
    val dxf = DxfBuilder()

    dxf.pair(999, "A-Cut 2Dpro - DXF export - units: mm - origin: bottom-left of each sheet")

    dxf.section("HEADER") {
        dxf.pair(9, "\$ACADVER").pair(1, "AC1009")
    }

    dxf.section("TABLES") {
        dxf.pair(0, "TABLE").pair(2, "LTYPE").pair(70, 1)
        dxf.pair(0, "LTYPE").pair(2, "CONTINUOUS").pair(70, 0).pair(3, "Solid line")
            .pair(72, 65).pair(73, 0).pair(40, 0.0)
        dxf.pair(0, "ENDTAB")

        dxf.pair(0, "TABLE").pair(2, "LAYER").pair(70, DXF_LAYERS.size)
        for ((name, color) in DXF_LAYERS) {
            dxf.pair(0, "LAYER").pair(2, name).pair(70, 0).pair(62, color).pair(6, "CONTINUOUS")
        }
        dxf.pair(0, "ENDTAB")

        dxf.pair(0, "TABLE").pair(2, "STYLE").pair(70, 1)
        dxf.pair(0, "STYLE").pair(2, "STANDARD").pair(70, 0).pair(40, 0.0).pair(41, 1.0)
            .pair(50, 0.0).pair(71, 0).pair(42, 2.5).pair(3, "txt").pair(4, "")
        dxf.pair(0, "ENDTAB")
    }

    dxf.section("BLOCKS") {}

    dxf.section("ENTITIES") {
        for (sheet in result.sheets) {
            val offsetX = sheet.sheetIndex * (stock.length + DXF_SHEET_GAP_MM)
            fun toX(x: Double) = offsetX + x

            dxf.closedPolyline(
                "SHEET",
                listOf(
                    offsetX to 0.0,
                    toX(stock.length) to 0.0,
                    toX(stock.length) to stock.width,
                    offsetX to stock.width,
                )
            )
            dxf.text(
                "LABEL",
                toX(stock.length / 2.0),
                stock.width + DXF_SHEET_GAP_MM * 0.35,
                min(45.0, max(18.0, stock.width * 0.018)),
                "SHEET ${sheet.sheetIndex + 1} - ${fmt(stock.length)} x ${fmt(stock.width)} mm",
            )

            sheet.placements.forEachIndexed { indexInSheet, p ->
                val y0 = stock.width - p.y - p.height
                val y1 = y0 + p.height
                val x0 = toX(p.x)
                val x1 = toX(p.x + p.width)

                dxf.closedPolyline(
                    "CUT",
                    listOf(
                        x0 to y0,
                        x1 to y0,
                        x1 to y1,
                        x0 to y1,
                    )
                )

                val source = partsById[p.id]
                if (source != null) {
                    val sides = getPartEdgeSides(source, p.isRotated)

                    val grooveInset = min(GROOVE_INSET_MM, min(p.width, p.height) / 4.0)
                    if (grooveInset > 0.0) {
                        for (side in sides.groove) {
                            when (side) {
                                PartSide.TOP -> dxf.line("GROOVE", x0, y1 - grooveInset, x1, y1 - grooveInset)
                                PartSide.BOTTOM -> dxf.line("GROOVE", x0, y0 + grooveInset, x1, y0 + grooveInset)
                                PartSide.LEFT -> dxf.line("GROOVE", x0 + grooveInset, y0, x0 + grooveInset, y1)
                                PartSide.RIGHT -> dxf.line("GROOVE", x1 - grooveInset, y0, x1 - grooveInset, y1)
                            }
                        }
                    }

                    val leg = min(30.0, (min(p.width, p.height) / 2.0) * 0.9)
                    if (leg >= 5.0 && sides.farsi.isNotEmpty()) {
                        val cornerDiagonals = mapOf(
                            "TL" to listOf(x0 + leg, y1, x0, y1 - leg),
                            "TR" to listOf(x1 - leg, y1, x1, y1 - leg),
                            "BL" to listOf(x0 + leg, y0, x0, y0 + leg),
                            "BR" to listOf(x1 - leg, y0, x1, y0 + leg),
                        )
                        val cornersBySide = mapOf(
                            PartSide.TOP to listOf("TL", "TR"),
                            PartSide.BOTTOM to listOf("BL", "BR"),
                            PartSide.LEFT to listOf("TL", "BL"),
                            PartSide.RIGHT to listOf("TR", "BR"),
                        )
                        val drawn = mutableSetOf<String>()
                        for (side in sides.farsi) {
                            for (corner in cornersBySide[side].orEmpty()) {
                                if (!drawn.add(corner)) continue
                                val coords = cornerDiagonals[corner] ?: continue
                                dxf.line("FARSI_45", coords[0], coords[1], coords[2], coords[3])
                            }
                        }
                    }
                }

                if (min(p.width, p.height) >= 25.0) {
                    dxf.text(
                        "LABEL",
                        (x0 + x1) / 2.0,
                        (y0 + y1) / 2.0,
                        min(45.0, max(10.0, min(p.width, p.height) * 0.14)),
                        getPlacementCode(sheet.sheetIndex, indexInSheet),
                        if (p.isRotated && p.height > p.width) 90.0 else 0.0,
                    )
                }
            }
        }
    }

    dxf.pair(0, "EOF")
    return dxf.build()
}

fun defaultDxfFileName(result: LayoutResult): String {
    return "ACut_${fmt(result.stock.length)}x${fmt(result.stock.width)}_${result.summary.sheetCount}sheets.dxf"
}
