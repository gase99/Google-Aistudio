package com.acut.cut2dpro.domain.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.UUID

/**
 * مدل دادهٔ قطعه (CutPart) — نسخهٔ نهایی با ورودی عددی ۰/۱/۲
 * ۰ = بدون عملیات، ۱ = یک لبه، ۲ = هر دو لبهٔ مقابل
 */
data class CutPart(
    val id: String = createPartId(),
    val name: String = "",
    val length: Double = 0.0,
    val width: Double = 0.0,
    val qty: Int = 1,
    val bandLength: Int = 0,
    val bandWidth: Int = 0,
    val farsiLength: Int = 0,
    val farsiWidth: Int = 0,
    val grooveLength: Int = 0,
    val grooveWidth: Int = 0,
)

enum class EdgeOperationGroup {
    BAND,
    FARSI,
    GROOVE,
}

data class MaterialSummary(
    val bandMeters: Double = 0.0,
    val farsiMeters: Double = 0.0,
    val grooveMeters: Double = 0.0,
)

fun createPartId(): String = UUID.randomUUID().toString()

fun normalizeEdgeCount(value: Int): Int = when {
    value >= 2 -> 2
    value >= 1 -> 1
    else -> 0
}

fun isEmptyPart(p: CutPart): Boolean {
    return p.name.trim().isEmpty() &&
        p.length <= 0.0 &&
        p.width <= 0.0 &&
        p.bandLength == 0 &&
        p.bandWidth == 0 &&
        p.farsiLength == 0 &&
        p.farsiWidth == 0 &&
        p.grooveLength == 0 &&
        p.grooveWidth == 0
}

fun isValidPart(p: CutPart): Boolean {
    return p.length > 0.0 && p.width > 0.0 && p.qty > 0
}

fun calculateMaterialSummary(parts: List<CutPart>): MaterialSummary {
    var band = 0.0
    var farsi = 0.0
    var groove = 0.0

    for (part in parts) {
        if (!part.length.isFinite() || !part.width.isFinite() ||
            part.length <= 0.0 || part.width <= 0.0 || part.qty <= 0
        ) {
            continue
        }
        val lengthMeters = (part.length * part.qty) / 1000.0
        val widthMeters = (part.width * part.qty) / 1000.0
        band += normalizeEdgeCount(part.bandLength) * lengthMeters +
            normalizeEdgeCount(part.bandWidth) * widthMeters
        farsi += normalizeEdgeCount(part.farsiLength) * lengthMeters +
            normalizeEdgeCount(part.farsiWidth) * widthMeters
        groove += normalizeEdgeCount(part.grooveLength) * lengthMeters +
            normalizeEdgeCount(part.grooveWidth) * widthMeters
    }

    return MaterialSummary(
        bandMeters = band,
        farsiMeters = farsi,
        grooveMeters = groove,
    )
}

fun createSampleParts(): List<CutPart> = listOf(
    CutPart(
        name = "بدنه کناری",
        length = 720.0,
        width = 560.0,
        qty = 2,
        bandLength = 2,
        grooveLength = 1,
    ),
    CutPart(
        name = "درب کابینت",
        length = 715.0,
        width = 396.0,
        qty = 4,
        bandLength = 2,
        bandWidth = 2,
        farsiLength = 2,
    ),
    CutPart(
        name = "طبقه",
        length = 564.0,
        width = 520.0,
        qty = 3,
        bandLength = 1,
    ),
    CutPart(
        name = "سقف و کف",
        length = 564.0,
        width = 560.0,
        qty = 2,
        bandLength = 2,
        grooveWidth = 1,
    ),
    CutPart(),
)

/** تبدیل ارقام فارسی/عربی به انگلیسی */
fun normalizeDigits(raw: String): String {
    val sb = StringBuilder(raw.length)
    for (ch in raw) {
        when (ch) {
            in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
            in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
            '٫', '،' -> sb.append('.')
            else -> sb.append(ch)
        }
    }
    return sb.toString()
}

fun parseDimensionInput(raw: String): Double {
    val cleaned = normalizeDigits(raw).filter { it.isDigit() || it == '.' }
    val value = cleaned.toDoubleOrNull() ?: 0.0
    return if (value.isFinite() && value >= 0.0) value else 0.0
}

fun parseQtyInput(raw: String): Int {
    val cleaned = normalizeDigits(raw).filter { it.isDigit() }
    return cleaned.toIntOrNull()?.coerceAtLeast(0) ?: 0
}

fun parseEdgeCountInput(raw: String): Int {
    val cleaned = normalizeDigits(raw).filter { it.isDigit() }
    if (cleaned.isEmpty()) return 0
    val n = cleaned.toIntOrNull() ?: 0
    return normalizeEdgeCount(n.coerceIn(0, 2))
}

fun formatDimValue(value: Double): String {
    if (value <= 0.0) return ""
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        DecimalFormat("0.##", DecimalFormatSymbols(Locale.US)).format(value)
    }
}

private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

fun toPersianDigits(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
        if (ch in '0'..'9') {
            sb.append(persianDigits[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

fun formatFaInt(value: Int): String {
    val formatted = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US)).format(value)
    return toPersianDigits(formatted.replace(',', '٬'))
}

fun formatFaDecimal(value: Double, maxFractionDigits: Int = 2): String {
    val pattern = if (maxFractionDigits <= 1) "#,##0.#" else "#,##0.##"
    val formatted = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US)).format(value)
    return toPersianDigits(formatted.replace(',', '٬').replace('.', '٫'))
}
