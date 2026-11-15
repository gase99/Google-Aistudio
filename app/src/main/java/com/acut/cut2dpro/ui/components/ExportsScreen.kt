package com.acut.cut2dpro.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.acut.cut2dpro.domain.export.PdfExporter
import com.acut.cut2dpro.domain.export.defaultDxfFileName
import com.acut.cut2dpro.domain.export.generateDxf
import com.acut.cut2dpro.domain.model.formatFaInt
import com.acut.cut2dpro.ui.MainUiState
import com.acut.cut2dpro.ui.theme.AmberGroove
import com.acut.cut2dpro.ui.theme.AmberGrooveText
import com.acut.cut2dpro.ui.theme.AmberGrooveTint
import com.acut.cut2dpro.ui.theme.EmeraldAction
import com.acut.cut2dpro.ui.theme.IndigoAccent
import com.acut.cut2dpro.ui.theme.RoseFarsi
import com.acut.cut2dpro.ui.theme.SkyPrimaryDark
import com.acut.cut2dpro.ui.theme.Slate100
import com.acut.cut2dpro.ui.theme.Slate200
import com.acut.cut2dpro.ui.theme.Slate600
import com.acut.cut2dpro.ui.theme.Slate700
import com.acut.cut2dpro.ui.theme.Slate900
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class PendingSaveKind {
    MAP_PDF,
    LABELS_PDF,
    DXF,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportsScreen(
    state: MainUiState,
    onRecalculate: () -> Unit,
    onGoToSetup: () -> Unit,
    onShowNotice: (String) -> Unit,
) {
    val layoutState = state.layoutState
    if (layoutState == null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Slate200),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "هنوز خروجی‌ای آماده نیست",
                    style = MaterialTheme.typography.titleMedium,
                    color = Slate900,
                )
                Text(
                    text = "پس از تعریف قطعات و محاسبهٔ چیدمان، می‌توانید خروجی PDF نقشه، لیبل‌ها و فایل DXF را ذخیره یا چاپ کنید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.canCalculate) {
                        Button(
                            onClick = onRecalculate,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAction),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("exports_empty_calculate_button"),
                        ) {
                            Text("محاسبه چیدمان")
                        }
                    }
                    OutlinedButton(
                        onClick = onGoToSetup,
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("بازگشت به تعریف قطعات")
                    }
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val result = layoutState.result
    val parts = layoutState.partsSnapshot
    val isStale = state.isStale
    var busy by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<PendingSaveKind?>(null) }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        val kind = pendingSave
        pendingSave = null
        if (uri == null || kind == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val file = when (kind) {
                        PendingSaveKind.MAP_PDF -> PdfExporter.generateMapPdf(context, result, parts)
                        PendingSaveKind.LABELS_PDF -> PdfExporter.generateLabelsPdf(context, result, parts)
                        PendingSaveKind.DXF -> null
                    }
                    if (file != null) {
                        PdfExporter.writeFileToUri(context, file, uri)
                    }
                }
                onShowNotice("فایل PDF با موفقیت ذخیره شد ✓")
            } catch (e: Exception) {
                onShowNotice("خطا در ذخیرهٔ PDF: ${e.localizedMessage ?: "نامشخص"}")
            } finally {
                busy = false
            }
        }
    }

    val createDxfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/dxf"),
    ) { uri ->
        pendingSave = null
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val dxfText = generateDxf(result, parts)
                    PdfExporter.writeTextToUri(context, dxfText, uri)
                }
                onShowNotice("فایل DXF با موفقیت ذخیره شد ✓")
            } catch (e: Exception) {
                onShowNotice("خطا در ذخیرهٔ DXF: ${e.localizedMessage ?: "نامشخص"}")
            } finally {
                busy = false
            }
        }
    }

    val actionsEnabled = !isStale && !busy

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (isStale) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = AmberGrooveTint,
                border = BorderStroke(1.5.dp, AmberGroove),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = AmberGroove)
                        Text(
                            text = "تغییرات جدید هنوز محاسبه نشده‌اند؛ برای جلوگیری از خروجی مغایر، ابتدا چیدمان را دوباره محاسبه کنید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmberGrooveText,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onRecalculate,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGroove),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("exports_stale_recalculate_button"),
                    ) {
                        Text("محاسبهٔ مجدد")
                    }
                }
            }
        }

        // 1) Map + Grouped Checklist PDF
        ExportOptionCard(
            icon = Icons.Filled.PictureAsPdf,
            accentColor = SkyPrimaryDark,
            title = "PDF نقشهٔ برش + فهرست تولید + فاکتور متریال",
            subtitle = "شامل ${formatFaInt(result.summary.sheetCount)} صفحه نقشهٔ سیاه‌وسفید استاندارد A4 به همراه جدول گروه‌بندی‌شدهٔ قطعات و متراژ نوار/شیار/فارسی.",
            badges = listOf("A4 چاپی", "نقشهٔ برش", "چک‌لیست تولید", "فاکتور متریال"),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val file = withContext(Dispatchers.IO) {
                                    PdfExporter.generateMapPdf(context, result, parts)
                                }
                                PdfExporter.shareFile(
                                    context,
                                    file,
                                    "application/pdf",
                                    "اشتراک‌گذاری PDF نقشهٔ برش",
                                )
                            } catch (e: Exception) {
                                onShowNotice("خطا در تولید PDF: ${e.localizedMessage ?: "نامشخص"}")
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = actionsEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = SkyPrimaryDark),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("export_map_pdf_share_button"),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("اشتراک‌گذاری")
                }

                OutlinedButton(
                    onClick = {
                        pendingSave = PendingSaveKind.MAP_PDF
                        createPdfLauncher.launch(PdfExporter.mapPdfFileName(result))
                    },
                    enabled = actionsEnabled,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("export_map_pdf_save_button"),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ذخیره در دستگاه")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val file = withContext(Dispatchers.IO) {
                                    PdfExporter.generateMapPdf(context, result, parts)
                                }
                                PdfExporter.printPdfFile(context, file, "A-Cut 2Dpro - نقشه برش")
                            } catch (e: Exception) {
                                onShowNotice("خطا در چاپ PDF: ${e.localizedMessage ?: "نامشخص"}")
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = actionsEnabled,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("export_map_pdf_print_button"),
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("چاپ مستقیم")
                }
            }
        }

        // 2) DXF R12 Export
        ExportOptionCard(
            icon = Icons.Filled.Architecture,
            accentColor = IndigoAccent,
            title = "فایل DXF R12 (اتوکد و دستگاه CNC)",
            subtitle = "فایل استاندارد AC1009 با محور مختصات گوشهٔ پایین-چپ هر ورق (واحد میلی‌متر) و لایه‌های مجزا برای برش، شیار، فارسی و برچسب.",
            badges = listOf("SHEET", "CUT", "GROOVE", "FARSI_45", "LABEL"),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val file = withContext(Dispatchers.IO) {
                                    val dxf = generateDxf(result, parts)
                                    PdfExporter.writeTextToCacheFile(context, defaultDxfFileName(result), dxf)
                                }
                                PdfExporter.shareFile(
                                    context,
                                    file,
                                    "application/octet-stream",
                                    "اشتراک‌گذاری فایل DXF",
                                )
                            } catch (e: Exception) {
                                onShowNotice("خطا در تولید DXF: ${e.localizedMessage ?: "نامشخص"}")
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = actionsEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("export_dxf_share_button"),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("اشتراک‌گذاری DXF")
                }

                OutlinedButton(
                    onClick = {
                        pendingSave = PendingSaveKind.DXF
                        createDxfLauncher.launch(defaultDxfFileName(result))
                    },
                    enabled = actionsEnabled,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("export_dxf_save_button"),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ذخیره در دستگاه")
                }
            }
        }

        // 3) 3x5 A4 Labels PDF
        ExportOptionCard(
            icon = Icons.Filled.Label,
            accentColor = RoseFarsi,
            title = "PDF برگهٔ لیبل قطعات (۳ × ۵ در A4)",
            subtitle = "چاپ ۱۵ برچسب در هر صفحه A4 شامل کد نقشه، ابعاد، جهت‌نمای گرافیکی عملیات لبه و مشخصات قطعه.",
            badges = listOf("۱۵ لیبل در برگه", "نمودار جهت‌نما", "آمادهٔ برش لیبل"),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val file = withContext(Dispatchers.IO) {
                                    PdfExporter.generateLabelsPdf(context, result, parts)
                                }
                                PdfExporter.shareFile(
                                    context,
                                    file,
                                    "application/pdf",
                                    "اشتراک‌گذاری PDF لیبل قطعات",
                                )
                            } catch (e: Exception) {
                                onShowNotice("خطا در تولید PDF لیبل: ${e.localizedMessage ?: "نامشخص"}")
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = actionsEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = RoseFarsi),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("export_labels_pdf_share_button"),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("اشتراک‌گذاری")
                }

                OutlinedButton(
                    onClick = {
                        pendingSave = PendingSaveKind.LABELS_PDF
                        createPdfLauncher.launch(PdfExporter.labelsPdfFileName(result))
                    },
                    enabled = actionsEnabled,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("export_labels_pdf_save_button"),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ذخیره در دستگاه")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val file = withContext(Dispatchers.IO) {
                                    PdfExporter.generateLabelsPdf(context, result, parts)
                                }
                                PdfExporter.printPdfFile(context, file, "A-Cut 2Dpro - لیبل قطعات")
                            } catch (e: Exception) {
                                onShowNotice("خطا در چاپ لیبل: ${e.localizedMessage ?: "نامشخص"}")
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = actionsEnabled,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("export_labels_pdf_print_button"),
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("چاپ مستقیم")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExportOptionCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    badges: List<String>,
    actions: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Slate200),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.12f),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate900,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600,
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                badges.forEach { badge ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate100,
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate700,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate200)
            actions()
        }
    }
}
