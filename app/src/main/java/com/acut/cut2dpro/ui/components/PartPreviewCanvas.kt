package com.acut.cut2dpro.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acut.cut2dpro.domain.model.CutPart
import com.acut.cut2dpro.domain.model.formatDimValue
import com.acut.cut2dpro.domain.model.formatFaInt
import com.acut.cut2dpro.domain.model.normalizeEdgeCount
import com.acut.cut2dpro.ui.theme.AmberGroove
import com.acut.cut2dpro.ui.theme.AmberGrooveSoft
import com.acut.cut2dpro.ui.theme.AmberGrooveText
import com.acut.cut2dpro.ui.theme.RoseFarsi
import com.acut.cut2dpro.ui.theme.RoseFarsiSoft
import com.acut.cut2dpro.ui.theme.RoseFarsiText
import com.acut.cut2dpro.ui.theme.Slate200
import com.acut.cut2dpro.ui.theme.Slate400
import com.acut.cut2dpro.ui.theme.Slate500
import com.acut.cut2dpro.ui.theme.Slate600
import com.acut.cut2dpro.ui.theme.Slate700
import com.acut.cut2dpro.ui.theme.Slate800
import com.acut.cut2dpro.ui.theme.Slate900
import com.acut.cut2dpro.ui.theme.SubtleSurface
import com.acut.cut2dpro.ui.theme.TealBand
import com.acut.cut2dpro.ui.theme.TealBandSoft
import com.acut.cut2dpro.ui.theme.TealBandText
import kotlin.math.max
import kotlin.math.min

@Composable
fun PartPreviewCard(part: CutPart?, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "پیش‌نمایش لبه‌های ردیف فعال",
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate800,
                )
                Text(
                    text = part?.let { "«${it.name.trim().ifEmpty { "بدون نام" }}»" } ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate400,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (part == null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                ) {
                    Text("ردیفی انتخاب نشده است.", color = Slate400)
                }
            } else {
                PartPreviewCanvas(
                    part = part,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildAnnotatedString {
                    append("در ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Slate800)) {
                        append("نقشهٔ برش")
                    }
                    append(" همین عملیات سیاه‌وسفید روی خودِ همان لبه علامت می‌خورد: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Slate900)) {
                        append("نوار مشکی توپر = نوار، کانال دو خطی = شیار، خط دندانه‌ای = فارسی ۴۵°")
                    }
                    append(" (با حرف «ن / ش / ف» کنار لبه)")
                },
                style = MaterialTheme.typography.labelSmall,
                color = Slate500,
            )
        }
    }
}

@Composable
fun PartPreviewCanvas(part: CutPart, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val margin = 42.dp.toPx()

        val length = if (part.length > 0.0) part.length.toFloat() else 600f
        val width = if (part.width > 0.0) part.width.toFloat() else 400f
        val availW = max(40f, w - margin * 2f)
        val availH = max(40f, h - margin * 2f)
        val scale = min(availW / length, availH / width)
        val rw = max(60.dp.toPx(), length * scale)
        val rh = max(44.dp.toPx(), width * scale)
        val x = (w - rw) / 2f
        val y = (h - rh) / 2f - 4.dp.toPx()

        // Part body
        drawRoundRect(
            color = SubtleSurface,
            topLeft = Offset(x, y),
            size = Size(rw, rh),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        )
        drawRoundRect(
            color = Slate500,
            topLeft = Offset(x, y),
            size = Size(rw, rh),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx()),
        )

        val grooveInset = 8.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 4.dp.toPx()), 0f)
        val gStroke = 2.dp.toPx()
        val gL = normalizeEdgeCount(part.grooveLength)
        val gW = normalizeEdgeCount(part.grooveWidth)

        if (gL >= 2) {
            drawLine(AmberGroove, Offset(x + 3.dp.toPx(), y + grooveInset), Offset(x + rw - 3.dp.toPx(), y + grooveInset), gStroke, pathEffect = dash)
            drawLine(AmberGroove, Offset(x + 3.dp.toPx(), y + rh - grooveInset), Offset(x + rw - 3.dp.toPx(), y + rh - grooveInset), gStroke, pathEffect = dash)
        } else if (gL == 1) {
            drawLine(AmberGroove, Offset(x + 3.dp.toPx(), y + grooveInset), Offset(x + rw - 3.dp.toPx(), y + grooveInset), gStroke, pathEffect = dash)
        }
        if (gW >= 2) {
            drawLine(AmberGroove, Offset(x + grooveInset, y + 3.dp.toPx()), Offset(x + grooveInset, y + rh - 3.dp.toPx()), gStroke, pathEffect = dash)
            drawLine(AmberGroove, Offset(x + rw - grooveInset, y + 3.dp.toPx()), Offset(x + rw - grooveInset, y + rh - 3.dp.toPx()), gStroke, pathEffect = dash)
        } else if (gW == 1) {
            drawLine(AmberGroove, Offset(x + grooveInset, y + 3.dp.toPx()), Offset(x + grooveInset, y + rh - 3.dp.toPx()), gStroke, pathEffect = dash)
        }

        // Band: thick teal edge lines
        val bStroke = 5.dp.toPx()
        val bL = normalizeEdgeCount(part.bandLength)
        val bW = normalizeEdgeCount(part.bandWidth)
        if (bL >= 2) {
            drawLine(TealBand, Offset(x, y), Offset(x + rw, y), bStroke, cap = StrokeCap.Square)
            drawLine(TealBand, Offset(x, y + rh), Offset(x + rw, y + rh), bStroke, cap = StrokeCap.Square)
        } else if (bL == 1) {
            drawLine(TealBand, Offset(x, y), Offset(x + rw, y), bStroke, cap = StrokeCap.Square)
        }
        if (bW >= 2) {
            drawLine(TealBand, Offset(x, y), Offset(x, y + rh), bStroke, cap = StrokeCap.Square)
            drawLine(TealBand, Offset(x + rw, y), Offset(x + rw, y + rh), bStroke, cap = StrokeCap.Square)
        } else if (bW == 1) {
            drawLine(TealBand, Offset(x, y), Offset(x, y + rh), bStroke, cap = StrokeCap.Square)
        }

        // Farsi 45°: rose edge + diagonal hatch + 45° labels
        val fStroke = 2.4.dp.toPx()
        val hatchStep = 11.dp.toPx()
        val hatchLen = 7.dp.toPx()
        val fL = normalizeEdgeCount(part.farsiLength)
        val fW = normalizeEdgeCount(part.farsiWidth)

        fun drawHorizHatch(yy: Float, dir: Float) {
            var tx = x + 6.dp.toPx()
            while (tx < x + rw - 2.dp.toPx()) {
                drawLine(RoseFarsi, Offset(tx, yy), Offset(tx - hatchLen * 0.7f, yy + dir * hatchLen), 1.4.dp.toPx(), cap = StrokeCap.Round)
                tx += hatchStep
            }
        }

        fun drawVertHatch(xx: Float, dir: Float) {
            var ty = y + 6.dp.toPx()
            while (ty < y + rh - 2.dp.toPx()) {
                drawLine(RoseFarsi, Offset(xx, ty), Offset(xx + dir * hatchLen, ty - hatchLen * 0.7f), 1.4.dp.toPx(), cap = StrokeCap.Round)
                ty += hatchStep
            }
        }

        val farsiTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = RoseFarsi.toArgb()
            textSize = 12.sp.toPx()
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val dimTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Slate700.toArgb()
            textSize = 12.sp.toPx()
            textAlign = Paint.Align.CENTER
        }

        if (fL >= 2) {
            drawLine(RoseFarsi, Offset(x, y), Offset(x + rw, y), fStroke)
            drawLine(RoseFarsi, Offset(x, y + rh), Offset(x + rw, y + rh), fStroke)
            drawHorizHatch(y, -1f)
            drawHorizHatch(y + rh, 1f)
            drawContext.canvas.nativeCanvas.drawText("۴۵°", x + rw / 2f, y - 12.dp.toPx(), farsiTextPaint)
            drawContext.canvas.nativeCanvas.drawText("۴۵°", x + rw / 2f, y + rh + 22.dp.toPx(), farsiTextPaint)
        } else if (fL == 1) {
            drawLine(RoseFarsi, Offset(x, y), Offset(x + rw, y), fStroke)
            drawHorizHatch(y, -1f)
            drawContext.canvas.nativeCanvas.drawText("۴۵°", x + rw / 2f, y - 12.dp.toPx(), farsiTextPaint)
        }

        if (fW >= 2) {
            drawLine(RoseFarsi, Offset(x, y), Offset(x, y + rh), fStroke)
            drawLine(RoseFarsi, Offset(x + rw, y), Offset(x + rw, y + rh), fStroke)
            drawVertHatch(x, -1f)
            drawVertHatch(x + rw, 1f)
            drawContext.canvas.nativeCanvas.apply {
                save()
                rotate(-90f, x - 14.dp.toPx(), y + rh / 2f)
                drawText("۴۵°", x - 14.dp.toPx(), y + rh / 2f + 4.dp.toPx(), farsiTextPaint)
                restore()
                save()
                rotate(-90f, x + rw + 14.dp.toPx(), y + rh / 2f)
                drawText("۴۵°", x + rw + 14.dp.toPx(), y + rh / 2f + 4.dp.toPx(), farsiTextPaint)
                restore()
            }
        } else if (fW == 1) {
            drawLine(RoseFarsi, Offset(x, y), Offset(x, y + rh), fStroke)
            drawVertHatch(x, -1f)
            drawContext.canvas.nativeCanvas.apply {
                save()
                rotate(-90f, x - 14.dp.toPx(), y + rh / 2f)
                drawText("۴۵°", x - 14.dp.toPx(), y + rh / 2f + 4.dp.toPx(), farsiTextPaint)
                restore()
            }
        }

        if (fL == 0) {
            val lLabel = if (part.length > 0.0) "${formatDimValue(part.length)} mm" else "طول؟"
            drawContext.canvas.nativeCanvas.drawText(lLabel, x + rw / 2f, y + rh + 20.dp.toPx(), dimTextPaint)
        }
        if (fW == 0) {
            val wLabel = if (part.width > 0.0) "${formatDimValue(part.width)} mm" else "عرض؟"
            drawContext.canvas.nativeCanvas.apply {
                save()
                rotate(-90f, x - 14.dp.toPx(), y + rh / 2f)
                drawText(wLabel, x - 14.dp.toPx(), y + rh / 2f + 4.dp.toPx(), dimTextPaint)
                restore()
            }
        }

        // Center name + dimensions + qty
        val centerNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Slate800.toArgb()
            textSize = 14.sp.toPx()
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val centerSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Slate500.toArgb()
            textSize = 11.5.sp.toPx()
            textAlign = Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText(
            part.name.trim().ifEmpty { "قطعهٔ بدون نام" },
            w / 2f,
            y + rh / 2f - 4.dp.toPx(),
            centerNamePaint,
        )
        drawContext.canvas.nativeCanvas.drawText(
            "${formatFaInt(length.toInt())} × ${formatFaInt(width.toInt())} — تعداد: ${formatFaInt(part.qty)}",
            w / 2f,
            y + rh / 2f + 16.dp.toPx(),
            centerSubPaint,
        )
    }
}

@Composable
fun EdgeLegendGuideCard(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            Text("راهنمای علائم", style = MaterialTheme.typography.titleSmall, color = Slate800)

            LegendGuideRow(
                badgeBg = TealBandSoft,
                badgeText = "ن",
                badgeColor = TealBand,
                title = "نوار (لبه‌چسب PVC)",
                titleColor = TealBandText,
                desc = "در پیش‌نمایش خط سبزآبی دارد؛ در نقشهٔ برش با نوار مشکی توپر روی لبه و حرف «ن» نشان داده می‌شود.",
            )
            LegendGuideRow(
                badgeBg = RoseFarsiSoft,
                badgeText = "۴۵°",
                badgeColor = RoseFarsi,
                title = "فارسی (برش ۴۵°)",
                titleColor = RoseFarsiText,
                desc = "روی نقشهٔ برش با خط دندانه‌ای (زیگزاگ ۴۵°) روی همان لبه و حرف «ف» کنارش نشان داده می‌شود.",
            )
            LegendGuideRow(
                badgeBg = AmberGrooveSoft,
                badgeText = "ش",
                badgeColor = AmberGroove,
                title = "شیار (پشت‌بند ۳ میل)",
                titleColor = AmberGrooveText,
                desc = "در پیش‌نمایش خط‌چین کهربایی است؛ در نقشهٔ برش با کانال دو خطی (خط‌چین وسط) و حرف «ش» می‌آید.",
            )

            Surface(
                color = SubtleSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Slate200),
            ) {
                Text(
                    text = "مقدار هر محور: ۰ بدون عملیات، ۱ یک لبه و ۲ هر دو لبهٔ مقابل است. «طول» لبه‌های موازی با طول قطعه و «عرض» لبه‌های موازی با عرض را مشخص می‌کند.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate600,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun LegendGuideRow(
    badgeBg: Color,
    badgeText: String,
    badgeColor: Color,
    title: String,
    titleColor: Color,
    desc: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(badgeBg),
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = badgeColor,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = titleColor)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = Slate600)
        }
    }
}
