package com.acut.cut2dpro.domain.algorithm

import com.acut.cut2dpro.domain.model.CutPart
import com.acut.cut2dpro.domain.model.formatDimValue
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class EdgeOp(val letter: String) {
    BAND("ن"),
    FARSI("ف"),
    GROOVE("ش"),
}

data class EdgeFrame(
    val x0: Double,
    val y0: Double,
    val ax: Double,
    val ay: Double,
    val nx: Double,
    val ny: Double,
    val len: Double,
) {
    fun at(t: Double, d: Double): Pair<Double, Double> =
        (x0 + ax * t + nx * d) to (y0 + ay * t + ny * d)

    fun bandRect(t0: Double, t1: Double, d0: Double, d1: Double): RectD {
        val (ax0, ay0) = at(t0, d0)
        val (bx0, by0) = at(t1, d1)
        return RectD(
            x = min(ax0, bx0),
            y = min(ay0, by0),
            width = abs(bx0 - ax0),
            height = abs(by0 - ay0),
        )
    }
}

data class RectD(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

fun edgeFrame(side: PartSide, x: Double, y: Double, w: Double, h: Double): EdgeFrame = when (side) {
    PartSide.TOP -> EdgeFrame(x, y, 1.0, 0.0, 0.0, 1.0, w)
    PartSide.BOTTOM -> EdgeFrame(x, y + h, 1.0, 0.0, 0.0, -1.0, w)
    PartSide.LEFT -> EdgeFrame(x, y, 0.0, 1.0, 1.0, 0.0, h)
    PartSide.RIGHT -> EdgeFrame(x + w, y, 0.0, 1.0, -1.0, 0.0, h)
}

data class EdgeLayout(
    val side: PartSide,
    val ops: List<EdgeOp>,
)

fun computeEdgeLayouts(source: CutPart?, isRotated: Boolean): List<EdgeLayout> {
    if (source == null) return emptyList()
    val sides = getPartEdgeSides(source, isRotated)
    val order = listOf(PartSide.TOP, PartSide.RIGHT, PartSide.BOTTOM, PartSide.LEFT)
    return order.mapNotNull { side ->
        val ops = buildList {
            if (sides.band.contains(side)) add(EdgeOp.BAND)
            if (sides.farsi.contains(side)) add(EdgeOp.FARSI)
            if (sides.groove.contains(side)) add(EdgeOp.GROOVE)
        }
        if (ops.isEmpty()) null else EdgeLayout(side, ops)
    }
}

data class MarkMetrics(
    val u: Double,
    val stroke: Double,
    val tagR: Double,
) {
    private val gap = u * 0.6
    private val sizes = mapOf(
        EdgeOp.BAND to u,
        EdgeOp.FARSI to u * 1.1,
        EdgeOp.GROOVE to u * 1.7,
    )

    fun zone(ops: List<EdgeOp>, op: EdgeOp): Pair<Double, Double> {
        var d = 0.0
        for (o in ops) {
            if (o != EdgeOp.BAND || d > 0.0) d += gap
            val s = sizes[o] ?: 0.0
            if (o == op) return d to (d + s)
            d += s
        }
        return 0.0 to 0.0
    }

    fun depth(ops: List<EdgeOp>): Double {
        if (ops.isEmpty()) return 0.0
        return zone(ops, ops.last()).second
    }
}

fun computeMarkMetrics(w: Double, h: Double, scale: Double): MarkMetrics {
    val minDim = min(w, h)
    val u = min(max(min(6.0 * scale, minDim * 0.075), 0.6 * scale), minDim * 0.08)
    val tagR = min(u * 1.35, 20.0 * scale)
    return MarkMetrics(
        u = u,
        stroke = max(0.5 * scale, u * 0.16),
        tagR = if (minDim >= 7.0 * tagR) tagR else 0.0,
    )
}

data class TagLayout(
    val cx: Double,
    val cy: Double,
    val halfW: Double,
)

fun computeTagLayout(
    edge: EdgeLayout,
    frame: EdgeFrame,
    m: MarkMetrics,
    w: Double,
    h: Double,
    cornerClear: Double,
    alongLimit: Double,
): TagLayout? {
    val r = m.tagR
    if (r <= 0.0) return null
    val halfW = r + (edge.ops.size - 1) * r * 0.85
    val vertical = frame.nx != 0.0
    val halfAcross = if (vertical) halfW else r
    val halfAlong = if (vertical) r else halfW
    val depth = max(halfAcross * 1.1, m.depth(edge.ops) / 2.0)
    val across = if (vertical) w else h
    if (depth + halfAcross > across / 2.0) return null
    val t = max(frame.len * 0.18, cornerClear + halfAlong + m.u * 0.5)
    if (t + halfAlong > alongLimit) return null
    val (cx, cy) = frame.at(t, depth)
    return TagLayout(cx = cx, cy = cy, halfW = halfW)
}

data class DimLabel(
    val side: PartSide,
    val text: String,
    val x: Double,
    val y: Double,
    val fontSize: Double,
    val rotate: Boolean,
    val half: Double,
    val reach: Double,
)

data class NameFit(
    val size: Double,
    val vertical: Boolean,
)

const val NAME_FONT_MAX_MM = 50.0
const val NAME_FONT_MIN_MM = 7.0
const val DIM_FONT_MAX_MM = 42.0
const val DIM_FONT_MIN_MM = 6.0

fun fitName(name: String, availW: Double, availH: Double): NameFit {
    val n = max(name.codePointCount(0, name.length), 1)
    fun fit(along: Double, across: Double): Double {
        if (along <= 0.0 || across <= 0.0) return 0.0
        return min(NAME_FONT_MAX_MM, min((along * 0.9) / (n * 0.6), across * 0.8))
    }
    val horizontal = fit(availW, availH)
    val vertical = fit(availH, availW)
    return if (vertical > horizontal * 1.15) {
        NameFit(vertical, true)
    } else {
        NameFit(horizontal, false)
    }
}

data class MarkedEdgeRender(
    val edge: EdgeLayout,
    val frame: EdgeFrame,
    val tag: TagLayout?,
)

data class PlacedPartGeometry(
    val name: String,
    val edges: List<MarkedEdgeRender>,
    val dims: List<DimLabel>,
    val metrics: MarkMetrics,
    val nameFit: NameFit,
    val nameX: Double,
    val nameY: Double,
    val showFallbackIndex: Boolean,
)

fun computePlacedPartGeometry(
    placement: PlacedPart,
    source: CutPart?,
    scale: Double,
): PlacedPartGeometry {
    val x = placement.x
    val y = placement.y
    val w = placement.width
    val h = placement.height
    val isRotated = placement.isRotated

    val name = (placement.name.ifBlank { source?.name ?: "" }).trim().ifEmpty { "بدون نام" }
    val partLength = source?.length ?: if (isRotated) h else w
    val partWidth = source?.width ?: if (isRotated) w else h

    val edges = computeEdgeLayouts(source, isRotated)
    val m = computeMarkMetrics(w, h, scale)

    fun markDepth(side: PartSide): Double {
        val e = edges.find { it.side == side }
        return if (e != null) m.depth(e.ops) else 0.0
    }

    val dimFont = min(DIM_FONT_MAX_MM, max(min(w, h) * 0.16, 2.5 * scale))
    fun dimLabel(side: PartSide, value: Double): DimLabel? {
        val text = formatDimValue(value).ifEmpty { value.toString() }
        val frame = edgeFrame(side, x, y, w, h)
        val across = if (side == PartSide.TOP) h else w
        val clear = if (side == PartSide.TOP) {
            max(markDepth(PartSide.LEFT), markDepth(PartSide.RIGHT))
        } else {
            max(markDepth(PartSide.TOP), markDepth(PartSide.BOTTOM))
        }
        val room = frame.len - 2.0 * clear
        val charLen = max(1, text.codePointCount(0, text.length))
        val font = min(dimFont, (room * 0.8) / (charLen * 0.6))
        if (font < DIM_FONT_MIN_MM) return null
        val depth = markDepth(side) + m.u * 0.6 + font * 0.55
        if (depth + font * 0.55 > across / 2.0) return null
        val (lx, ly) = frame.at(frame.len / 2.0, depth)
        return DimLabel(
            side = side,
            text = text,
            x = lx,
            y = ly,
            fontSize = font,
            rotate = side == PartSide.RIGHT,
            half = (charLen * 0.6 * font) / 2.0,
            reach = depth + font * 0.55,
        )
    }

    val dims = listOfNotNull(
        dimLabel(PartSide.TOP, if (isRotated) partWidth else partLength),
        dimLabel(PartSide.RIGHT, if (isRotated) partLength else partWidth),
    )

    val marked = edges.map { edge ->
        val frame = edgeFrame(edge.side, x, y, w, h)
        val vertical = edge.side == PartSide.LEFT || edge.side == PartSide.RIGHT
        val cornerClear = if (vertical) {
            max(markDepth(PartSide.TOP), markDepth(PartSide.BOTTOM))
        } else {
            max(markDepth(PartSide.LEFT), markDepth(PartSide.RIGHT))
        }
        val dim = dims.find { it.side == edge.side }
        val alongLimit = if (dim != null) {
            frame.len / 2.0 - dim.half - m.u
        } else {
            frame.len - cornerClear - m.u * 0.5
        }
        MarkedEdgeRender(
            edge = edge,
            frame = frame,
            tag = computeTagLayout(edge, frame, m, w, h, cornerClear, alongLimit),
        )
    }

    fun reachOf(side: PartSide): Double {
        val e = marked.find { it.edge.side == side }
        val tag = e?.tag
        val tagReach = if (tag != null) {
            when (side) {
                PartSide.LEFT -> abs(tag.cx - x) + tag.halfW
                PartSide.RIGHT -> abs(tag.cx - (x + w)) + tag.halfW
                PartSide.TOP -> abs(tag.cy - y) + m.tagR
                PartSide.BOTTOM -> abs(tag.cy - (y + h)) + m.tagR
            }
        } else {
            0.0
        }
        val dimReach = dims.find { it.side == side }?.reach ?: 0.0
        return max(markDepth(side), max(tagReach, dimReach))
    }

    val gapIn = m.u * 0.6
    val innerX0 = x + reachOf(PartSide.LEFT) + gapIn
    val innerX1 = x + w - reachOf(PartSide.RIGHT) - gapIn
    val innerY0 = y + reachOf(PartSide.TOP) + gapIn
    val innerY1 = y + h - reachOf(PartSide.BOTTOM) - gapIn
    val nameFit = fitName(name, innerX1 - innerX0, innerY1 - innerY0)

    return PlacedPartGeometry(
        name = name,
        edges = marked,
        dims = dims,
        metrics = m,
        nameFit = nameFit,
        nameX = (innerX0 + innerX1) / 2.0,
        nameY = (innerY0 + innerY1) / 2.0,
        showFallbackIndex = nameFit.size < NAME_FONT_MIN_MM && min(w, h) >= 14.0 && dims.isEmpty(),
    )
}

fun buildFarsiZigzagPoints(frame: EdgeFrame, d0: Double, d1: Double, inset: Double): List<Pair<Double, Double>> {
    val amp = d1 - d0
    val step = amp * 2.0
    val t0 = inset
    val t1 = frame.len - inset
    if (t1 <= t0 || step <= 0.0) return emptyList()
    val count = max(2, floor((t1 - t0) / step).toInt())
    val realStep = (t1 - t0) / count
    return List(count * 2 + 1) { i ->
        val t = t0 + (realStep / 2.0) * i
        frame.at(t, if (i % 2 == 0) d1 else d0)
    }
}
