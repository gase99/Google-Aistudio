package com.acut.cut2dpro.domain.algorithm

import com.acut.cut2dpro.domain.model.CutPart
import com.acut.cut2dpro.domain.model.normalizeEdgeCount

enum class PartSide {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
}

data class PartEdgeSides(
    val band: List<PartSide>,
    val farsi: List<PartSide>,
    val groove: List<PartSide>,
)

const val GROOVE_INSET_MM = 20.0

fun sidesForFlags(
    lengthCount: Int,
    widthCount: Int,
    isRotated: Boolean,
): List<PartSide> {
    val sides = mutableListOf<PartSide>()
    val lc = normalizeEdgeCount(lengthCount)
    val wc = normalizeEdgeCount(widthCount)

    if (lc >= 2) {
        if (isRotated) {
            sides.add(PartSide.LEFT)
            sides.add(PartSide.RIGHT)
        } else {
            sides.add(PartSide.TOP)
            sides.add(PartSide.BOTTOM)
        }
    } else if (lc == 1) {
        sides.add(if (isRotated) PartSide.LEFT else PartSide.TOP)
    }

    if (wc >= 2) {
        if (isRotated) {
            sides.add(PartSide.TOP)
            sides.add(PartSide.BOTTOM)
        } else {
            sides.add(PartSide.LEFT)
            sides.add(PartSide.RIGHT)
        }
    } else if (wc == 1) {
        sides.add(if (isRotated) PartSide.TOP else PartSide.LEFT)
    }

    return sides
}

fun getPartEdgeSides(part: CutPart, isRotated: Boolean): PartEdgeSides {
    return PartEdgeSides(
        band = sidesForFlags(part.bandLength, part.bandWidth, isRotated),
        farsi = sidesForFlags(part.farsiLength, part.farsiWidth, isRotated),
        groove = sidesForFlags(part.grooveLength, part.grooveWidth, isRotated),
    )
}

fun getPlacementCode(sheetIndex: Int, indexInSheet: Int): String {
    return "S${sheetIndex + 1}-${(indexInSheet + 1).toString().padStart(2, '0')}"
}
