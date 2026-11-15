package com.acut.cut2dpro.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.acut.cut2dpro.domain.model.Project
import com.acut.cut2dpro.domain.model.formatDimValue
import com.acut.cut2dpro.domain.model.formatFaInt
import com.acut.cut2dpro.domain.model.isValidPart
import com.acut.cut2dpro.ui.theme.AmberGrooveSoft
import com.acut.cut2dpro.ui.theme.AmberGrooveText
import com.acut.cut2dpro.ui.theme.EmeraldAction
import com.acut.cut2dpro.ui.theme.EmeraldSoft
import com.acut.cut2dpro.ui.theme.EmeraldText
import com.acut.cut2dpro.ui.theme.IndigoAccent
import com.acut.cut2dpro.ui.theme.RoseFarsi
import com.acut.cut2dpro.ui.theme.RoseFarsiTint
import com.acut.cut2dpro.ui.theme.SkyPrimary
import com.acut.cut2dpro.ui.theme.SkyPrimaryDark
import com.acut.cut2dpro.ui.theme.SkySoft
import com.acut.cut2dpro.ui.theme.SkySoftBorder
import com.acut.cut2dpro.ui.theme.Slate100
import com.acut.cut2dpro.ui.theme.Slate200
import com.acut.cut2dpro.ui.theme.Slate400
import com.acut.cut2dpro.ui.theme.Slate500
import com.acut.cut2dpro.ui.theme.Slate600
import com.acut.cut2dpro.ui.theme.Slate800
import com.acut.cut2dpro.ui.theme.Slate900
import com.acut.cut2dpro.ui.theme.SubtleSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TopHeaderBar(
    currentProjectId: String?,
    currentProjectName: String,
    isDirty: Boolean,
    saveNotice: String?,
    projects: List<Project>,
    onSaveDirect: () -> Unit,
    onSaveWithName: (name: String, saveAs: Boolean) -> Unit,
    onNewProject: () -> Unit,
    onOpenProject: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showProjectList by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var saveDialogMode by remember { mutableStateOf<Boolean?>(null) } // null = closed, false = save new, true = save as
    var pendingConfirmAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    fun guardDirty(actionLabel: String, block: () -> Unit) {
        if (!isDirty) {
            block()
        } else {
            pendingConfirmAction = actionLabel to block
        }
    }

    Surface(
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                // App Logo Mark
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(SkyPrimary, IndigoAccent))),
                ) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val s = size.minDimension
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(s * 0.1f, s * 0.1f),
                            size = Size(s * 0.8f, s * 0.8f),
                            cornerRadius = CornerRadius(s * 0.12f, s * 0.12f),
                            style = Stroke(width = s * 0.09f),
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(s * 0.52f, s * 0.1f),
                            end = Offset(s * 0.52f, s * 0.9f),
                            strokeWidth = s * 0.08f,
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(s * 0.1f, s * 0.58f),
                            end = Offset(s * 0.52f, s * 0.58f),
                            strokeWidth = s * 0.08f,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "A-Cut 2Dpro",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                            ),
                            color = Slate900,
                            maxLines = 1,
                        )
                        IconButton(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("about_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "دربارهٔ برنامه و توسعه‌دهنده",
                                tint = Slate400,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = currentProjectName,
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        val statusText = saveNotice ?: when {
                            isDirty -> "● تغییرات ذخیره‌نشده"
                            currentProjectId != null -> "✓ ذخیره‌شده"
                            else -> "پیش‌نویس خودکار"
                        }
                        val statusColor = when {
                            saveNotice != null -> EmeraldText
                            isDirty -> AmberGrooveText
                            currentProjectId != null -> EmeraldText
                            else -> Slate500
                        }
                        val statusBg = when {
                            saveNotice != null -> EmeraldSoft
                            isDirty -> AmberGrooveSoft
                            currentProjectId != null -> EmeraldSoft
                            else -> Slate100
                        }
                        Surface(
                            color = statusBg,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                Box {
                    Button(
                        onClick = { menuExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("project_manager_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "پروژه‌ها",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        if (isDirty) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFCD34D))
                            )
                        }
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.width(260.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SubtleSurface)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "پروژهٔ جاری",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400,
                            )
                            Text(
                                text = currentProjectName,
                                style = MaterialTheme.typography.titleSmall,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("ذخیرهٔ پروژه (Save)") },
                            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = SkyPrimaryDark) },
                            onClick = {
                                menuExpanded = false
                                if (currentProjectId == null) {
                                    saveDialogMode = false
                                } else {
                                    onSaveDirect()
                                }
                            },
                            modifier = Modifier.testTag("menu_save_project"),
                        )
                        DropdownMenuItem(
                            text = { Text("ذخیره با نام جدید… (Save As)") },
                            leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null, tint = Slate500) },
                            onClick = {
                                menuExpanded = false
                                saveDialogMode = true
                            },
                            modifier = Modifier.testTag("menu_save_as_project"),
                        )
                        DropdownMenuItem(
                            text = { Text("پروژهٔ جدید") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldAction) },
                            onClick = {
                                menuExpanded = false
                                guardDirty("شروع پروژهٔ جدید", onNewProject)
                            },
                            modifier = Modifier.testTag("menu_new_project"),
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("باز کردن پروژه…")
                                    Surface(color = Slate100, shape = RoundedCornerShape(999.dp)) {
                                        Text(
                                            text = formatFaInt(projects.size),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate600,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFFD97706)) },
                            onClick = {
                                menuExpanded = false
                                showProjectList = true
                            },
                            modifier = Modifier.testTag("menu_open_project"),
                        )

                        if (projects.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "اخیراً استفاده‌شده",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            )
                            projects.take(5).forEach { proj ->
                                val isCurrent = proj.id == currentProjectId
                                val totalQty = proj.parts.filter(::isValidPart).sumOf { it.qty }
                                DropdownMenuItem(
                                    enabled = !isCurrent,
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                text = proj.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (isCurrent) "فعلی" else "${formatFaInt(totalQty)} قطعه",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isCurrent) SkyPrimaryDark else Slate400,
                                            )
                                        }
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        guardDirty("باز کردن پروژهٔ دیگر") { onOpenProject(proj) }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = Slate200)
        }
    }

    // Save / Save As Name Dialog
    saveDialogMode?.let { isSaveAs ->
        val suggested = if (isSaveAs) {
            "$currentProjectName - کپی"
        } else if (currentProjectName == "پروژهٔ بدون نام") {
            "پروژهٔ جدید"
        } else {
            currentProjectName
        }
        var nameInput by remember(isSaveAs) { mutableStateOf(suggested) }
        AlertDialog(
            onDismissRequest = { saveDialogMode = null },
            title = { Text(if (isSaveAs) "نام نسخهٔ جدید پروژه" else "نام پروژه را وارد کنید") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    label = { Text("نام پروژه") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input"),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveDialogMode = null
                        onSaveWithName(nameInput, isSaveAs)
                    },
                    modifier = Modifier.testTag("confirm_save_project_button"),
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { saveDialogMode = null }) {
                    Text("انصراف")
                }
            },
        )
    }

    // Dirty Guard Confirmation Dialog
    pendingConfirmAction?.let { (actionLabel, callback) ->
        AlertDialog(
            onDismissRequest = { pendingConfirmAction = null },
            title = { Text("تغییرات ذخیره‌نشده") },
            text = {
                Text("پروژهٔ فعلی («$currentProjectName») تغییرات ذخیره‌نشده دارد. با $actionLabel این تغییرات از دست می‌رود. ادامه می‌دهید؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingConfirmAction = null
                        callback()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseFarsi),
                ) {
                    Text("بله، ادامه بده")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmAction = null }) {
                    Text("بازگشت")
                }
            },
        )
    }

    // Delete Confirmation Dialog
    projectToDelete?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("حذف پروژه") },
            text = { Text("پروژهٔ «${proj.name}» حذف شود؟ این عمل قابل بازگشت نیست.") },
            confirmButton = {
                Button(
                    onClick = {
                        projectToDelete = null
                        onDeleteProject(proj)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseFarsi),
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("انصراف")
                }
            },
        )
    }

    // Saved Projects Modal
    if (showProjectList) {
        ProjectListDialog(
            projects = projects,
            currentProjectId = currentProjectId,
            onDismiss = { showProjectList = false },
            onOpen = { proj ->
                showProjectList = false
                guardDirty("باز کردن پروژهٔ دیگر") { onOpenProject(proj) }
            },
            onDelete = { proj -> projectToDelete = proj },
        )
    }

    // Developer / About Modal
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun ProjectListDialog(
    projects: List<Project>,
    currentProjectId: String?,
    onDismiss: () -> Unit,
    onOpen: (Project) -> Unit,
    onDelete: (Project) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(projects, query) {
        val q = query.trim()
        if (q.isEmpty()) projects else projects.filter { it.name.contains(q, ignoreCase = true) }
    }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 580.dp),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SubtleSurface)
                        .padding(16.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SkySoft),
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = SkyPrimaryDark)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("پروژه‌های ذخیره‌شده", style = MaterialTheme.typography.titleMedium, color = Slate900)
                        Text(
                            "${formatFaInt(projects.size)} پروژه در دیتابیس محلی دستگاه",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }
                HorizontalDivider(color = Slate200)

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("جستجو در نام پروژه…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("project_search_input"),
                )

                if (filtered.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (projects.isEmpty()) "هنوز پروژه‌ای ذخیره نشده است" else "پروژه‌ای با این نام پیدا نشد",
                            style = MaterialTheme.typography.titleSmall,
                            color = Slate600,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (projects.isEmpty()) {
                                "از منوی «پروژه‌ها» گزینهٔ «ذخیرهٔ پروژه» را بزنید تا اینجا دیده شود."
                            } else {
                                "عبارت جستجو را تغییر دهید."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400,
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        items(filtered, key = { it.id }) { proj ->
                            val isCurrent = proj.id == currentProjectId
                            val valid = proj.parts.filter(::isValidPart)
                            val totalQty = valid.sumOf { it.qty }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isCurrent) SkySoft.copy(alpha = 0.45f) else Color.White,
                                border = BorderStroke(1.dp, if (isCurrent) SkySoftBorder else Slate200),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Text(
                                                text = proj.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Slate900,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false),
                                            )
                                            if (isCurrent) {
                                                Surface(color = SkyPrimaryDark, shape = RoundedCornerShape(999.dp)) {
                                                    Text(
                                                        "فعلی",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                    )
                                                }
                                            }
                                            if (proj.lastLayout != null) {
                                                Surface(color = EmeraldSoft, shape = RoundedCornerShape(999.dp)) {
                                                    Text(
                                                        "نقشه ✓",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = EmeraldText,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "ویرایش: ${dateFormat.format(Date(proj.updatedAt))} · ${formatFaInt(valid.size)} ردیف · ${formatFaInt(totalQty)} قطعه",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate500,
                                        )
                                        Text(
                                            text = "ورق: ${formatDimValue(proj.settings.stockLength)} × ${formatDimValue(proj.settings.stockWidth)} mm",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate400,
                                        )
                                    }

                                    Button(
                                        onClick = { onOpen(proj) },
                                        enabled = !isCurrent,
                                        colors = ButtonDefaults.buttonColors(containerColor = SkyPrimaryDark),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.heightIn(min = 44.dp),
                                    ) {
                                        Text(if (isCurrent) "باز است" else "باز کردن", style = MaterialTheme.typography.labelLarge)
                                    }

                                    IconButton(
                                        onClick = { onDelete(proj) },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(RoseFarsiTint),
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف ${proj.name}", tint = RoseFarsi)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate200)
                Text(
                    text = "داده‌ها کاملاً آفلاین و روی همین دستگاه ذخیره می‌شوند؛ حساب کاربری یا ارسال به اینترنت وجود ندارد.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("A-Cut 2Dpro — دربارهٔ برنامه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "بهینه‌ساز هوشمند برش دوبعدی ورق برای کارگاه‌های چوب، MDF، شیشه و فلز با الگوریتم گیلوتینی و محاسبهٔ ضخامت تیغه (Kerf).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600,
                )
                HorizontalDivider(color = Slate200)
                Text("طراحی و توسعه: عباس کروشاد", style = MaterialTheme.typography.titleSmall, color = Slate900)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { openUrl("https://instagram.com/Webxplusx") },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("اینستاگرام @Webxplusx")
                    }
                    OutlinedButton(
                        onClick = { openUrl("https://t.me/Webxplusx") },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("تلگرام @Webxplusx")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        },
    )
}
