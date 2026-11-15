package com.acut.cut2dpro.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.acut.cut2dpro.data.local.AppDatabase
import com.acut.cut2dpro.data.local.WorkspaceDraft
import com.acut.cut2dpro.data.local.WorkspaceDraftStore
import com.acut.cut2dpro.data.repository.ProjectRepository
import com.acut.cut2dpro.domain.algorithm.LayoutOptions
import com.acut.cut2dpro.domain.algorithm.StockSheet
import com.acut.cut2dpro.domain.algorithm.calculateLayout
import com.acut.cut2dpro.domain.model.CutPart
import com.acut.cut2dpro.domain.model.DEFAULT_LAYOUT_SETTINGS
import com.acut.cut2dpro.domain.model.LayoutSettings
import com.acut.cut2dpro.domain.model.LayoutState
import com.acut.cut2dpro.domain.model.Project
import com.acut.cut2dpro.domain.model.UNTITLED_PROJECT_NAME
import com.acut.cut2dpro.domain.model.computeLayoutSignature
import com.acut.cut2dpro.domain.model.computeWorkspaceSignature
import com.acut.cut2dpro.domain.model.createPartId
import com.acut.cut2dpro.domain.model.createProjectId
import com.acut.cut2dpro.domain.model.createSampleParts
import com.acut.cut2dpro.domain.model.isEmptyPart
import com.acut.cut2dpro.domain.model.isValidPart
import com.acut.cut2dpro.domain.model.normalizeEdgeCount
import com.acut.cut2dpro.domain.model.settingsFromLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppStep(val numberFa: String, val label: String) {
    SETUP("۱", "تعریف ورق و قطعات"),
    ANALYSIS("۲", "تحلیل و نقشه"),
    LABELS("۳", "لیبل‌ها"),
    EXPORTS("۴", "خروجی‌ها"),
}

data class PartStats(
    val rowCount: Int = 0,
    val totalQty: Int = 0,
    val totalAreaM2: Double = 0.0,
    val bandMeters: Double = 0.0,
    val grooveMeters: Double = 0.0,
    val farsiEdges: Int = 0,
    val incomplete: Int = 0,
)

data class MainUiState(
    val ready: Boolean = false,
    val activeStep: AppStep = AppStep.SETUP,
    val rows: List<CutPart> = listOf(CutPart()),
    val activePartId: String? = null,
    val focusTarget: Pair<String, String>? = null,
    val settings: LayoutSettings = DEFAULT_LAYOUT_SETTINGS,
    val layoutState: LayoutState? = null,
    val currentProjectId: String? = null,
    val currentProjectName: String = UNTITLED_PROJECT_NAME,
    val baselineSignature: String = "",
    val saveNotice: String? = null,
    val layoutError: String? = null,
    val checkedCodes: Set<String> = emptySet(),
    val mapZoomPercent: Int = 100,
) {
    val workspaceSignature: String
        get() = computeWorkspaceSignature(rows, settings, currentProjectName)

    val isDirty: Boolean
        get() = ready && workspaceSignature != baselineSignature

    val currentLayoutSignature: String
        get() = computeLayoutSignature(rows, settings)

    val isStale: Boolean
        get() = layoutState != null && layoutState.signature != currentLayoutSignature

    val validParts: List<CutPart>
        get() = rows.filter(::isValidPart)

    val canCalculate: Boolean
        get() = validParts.isNotEmpty() && settings.stockLength > 0.0 && settings.stockWidth > 0.0

    val stats: PartStats
        get() {
            val valid = validParts
            val totalQty = valid.sumOf { it.qty }
            val totalAreaM2 = valid.sumOf { it.length * it.width * it.qty } / 1_000_000.0
            val bandMeters = valid.sumOf {
                it.qty * (normalizeEdgeCount(it.bandLength) * it.length + normalizeEdgeCount(it.bandWidth) * it.width)
            } / 1000.0
            val grooveMeters = valid.sumOf {
                it.qty * (normalizeEdgeCount(it.grooveLength) * it.length + normalizeEdgeCount(it.grooveWidth) * it.width)
            } / 1000.0
            val farsiEdges = valid.sumOf {
                it.qty * (normalizeEdgeCount(it.farsiLength) + normalizeEdgeCount(it.farsiWidth))
            }
            val incomplete = rows.count { !isEmptyPart(it) && !isValidPart(it) }
            return PartStats(
                rowCount = valid.size,
                totalQty = totalQty,
                totalAreaM2 = totalAreaM2,
                bandMeters = bandMeters,
                grooveMeters = grooveMeters,
                farsiEdges = farsiEdges,
                incomplete = incomplete,
            )
        }

    val activePart: CutPart?
        get() = rows.find { it.id == activePartId } ?: rows.firstOrNull()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProjectRepository

    init {
        val db = AppDatabase.getInstance(application)
        val draftStore = WorkspaceDraftStore(application)
        repository = ProjectRepository(db.projectDao(), draftStore)
    }

    val projects: StateFlow<List<Project>> = repository.allProjects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val draft = repository.loadDraft()
            val activeId = (draft.parts.find(::isEmptyPart) ?: draft.parts.firstOrNull())?.id
            _uiState.update {
                it.copy(
                    ready = true,
                    rows = draft.parts,
                    activePartId = activeId,
                    settings = draft.settings,
                    layoutState = draft.layoutState,
                    currentProjectId = draft.projectId,
                    currentProjectName = draft.projectName,
                    baselineSignature = draft.baselineSignature,
                )
            }
        }
    }

    private fun persistDraft(state: MainUiState) {
        if (!state.ready) return
        viewModelScope.launch {
            repository.saveDraft(
                WorkspaceDraft(
                    projectId = state.currentProjectId,
                    projectName = state.currentProjectName,
                    parts = state.rows,
                    settings = state.settings,
                    layoutState = state.layoutState,
                    baselineSignature = state.baselineSignature,
                )
            )
        }
    }

    private fun updateAndSave(transform: (MainUiState) -> MainUiState) {
        _uiState.update { prev ->
            val next = transform(prev)
            persistDraft(next)
            next
        }
    }

    fun setStep(step: AppStep) {
        _uiState.update { it.copy(activeStep = step) }
    }

    fun setActivePart(id: String) {
        _uiState.update { it.copy(activePartId = id) }
    }

    fun clearFocusTarget() {
        _uiState.update { it.copy(focusTarget = null) }
    }

    fun updatePart(id: String, transform: (CutPart) -> CutPart) {
        updateAndSave { state ->
            state.copy(
                rows = state.rows.map { if (it.id == id) transform(it) else it },
                layoutError = null,
            )
        }
    }

    fun insertRowAfter(afterId: String?, duplicateSource: CutPart? = null, requestFocus: Boolean = true) {
        val fresh = if (duplicateSource != null) {
            duplicateSource.copy(id = createPartId())
        } else {
            CutPart()
        }
        updateAndSave { state ->
            val list = state.rows.toMutableList()
            val idx = if (afterId != null) list.indexOfFirst { it.id == afterId } else -1
            if (idx >= 0) {
                list.add(idx + 1, fresh)
            } else {
                list.add(fresh)
            }
            state.copy(
                rows = list,
                activePartId = fresh.id,
                focusTarget = if (requestFocus) fresh.id to "name" else null,
                layoutError = null,
            )
        }
    }

    fun deleteRow(id: String) {
        updateAndSave { state ->
            if (state.rows.size <= 1) return@updateAndSave state
            val idx = state.rows.indexOfFirst { it.id == id }
            val filtered = state.rows.filterNot { it.id == id }
            val neighbor = filtered.getOrNull(idx) ?: filtered.getOrNull(idx - 1) ?: filtered.firstOrNull()
            state.copy(
                rows = filtered,
                activePartId = neighbor?.id,
                layoutError = null,
            )
        }
    }

    fun loadSampleParts() {
        val sample = createSampleParts()
        val firstEmpty = sample.find(::isEmptyPart) ?: sample.first()
        updateAndSave { state ->
            state.copy(
                rows = sample,
                activePartId = firstEmpty.id,
                layoutError = null,
            )
        }
    }

    fun removeEmptyRows() {
        updateAndSave { state ->
            val kept = state.rows.filterNot(::isEmptyPart)
            val next = kept.ifEmpty { listOf(CutPart()) }
            val active = if (next.any { it.id == state.activePartId }) state.activePartId else next.first().id
            state.copy(
                rows = next,
                activePartId = active,
            )
        }
    }

    fun clearAllRows() {
        val fresh = CutPart()
        updateAndSave { state ->
            state.copy(
                rows = listOf(fresh),
                activePartId = fresh.id,
                layoutError = null,
            )
        }
    }

    fun updateSettings(transform: (LayoutSettings) -> LayoutSettings) {
        updateAndSave { state ->
            state.copy(
                settings = transform(state.settings),
                layoutError = null,
            )
        }
    }

    fun setZoom(percent: Int) {
        _uiState.update { it.copy(mapZoomPercent = percent.coerceIn(50, 300)) }
    }

    fun toggleChecklistCode(code: String) {
        _uiState.update { state ->
            val next = state.checkedCodes.toMutableSet()
            if (!next.add(code)) next.remove(code)
            state.copy(checkedCodes = next)
        }
    }

    fun calculate(navigateToAnalysis: Boolean = false) {
        val state = _uiState.value
        val valid = state.validParts
        if (valid.isEmpty()) {
            _uiState.update {
                it.copy(layoutError = "هنوز هیچ ردیف کاملی (طول، عرض و تعداد) در جدول وارد نشده است.")
            }
            return
        }
        if (state.settings.stockLength <= 0.0 || state.settings.stockWidth <= 0.0) {
            _uiState.update {
                it.copy(layoutError = "ابعاد صفحهٔ خام باید بزرگ‌تر از صفر باشد.")
            }
            return
        }
        val invalidBand = valid.any { part ->
            part.length - normalizeEdgeCount(part.bandWidth) * state.settings.bandThickness <= 0.0 ||
                part.width - normalizeEdgeCount(part.bandLength) * state.settings.bandThickness <= 0.0
        }
        if (invalidBand) {
            _uiState.update {
                it.copy(
                    layoutError = "پس از کسر ضخامت نوار، ابعاد یک یا چند قطعه صفر یا منفی می‌شود؛ ضخامت نوار یا تعداد لبه‌ها را بررسی کنید."
                )
            }
            return
        }

        val result = calculateLayout(
            parts = state.rows,
            stock = StockSheet(state.settings.stockLength, state.settings.stockWidth),
            options = LayoutOptions(
                kerf = state.settings.kerf,
                bandThickness = state.settings.bandThickness,
                allowRotation = state.settings.allowRotation,
            ),
        )
        val newState = LayoutState(
            result = result,
            partsSnapshot = state.rows.toList(),
            signature = state.currentLayoutSignature,
        )
        updateAndSave { s ->
            s.copy(
                layoutState = newState,
                layoutError = null,
                checkedCodes = emptySet(),
                activeStep = if (navigateToAnalysis) AppStep.ANALYSIS else s.activeStep,
            )
        }
        showNotice("چیدمان محاسبه شد ✓")
    }

    fun saveProjectWithName(nameInput: String?, saveAs: Boolean) {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val existing = projects.value.find { it.id == state.currentProjectId }
        val isNewOrCopy = state.currentProjectId == null || saveAs
        val projectId = if (isNewOrCopy) createProjectId() else state.currentProjectId!!
        val finalName = if (isNewOrCopy) {
            nameInput?.trim().orEmpty().ifEmpty {
                if (saveAs) "${state.currentProjectName} - کپی" else "پروژهٔ جدید"
            }
        } else {
            state.currentProjectName
        }
        val createdAt = if (isNewOrCopy) now else (existing?.createdAt ?: now)

        val project = Project(
            id = projectId,
            name = finalName,
            createdAt = createdAt,
            updatedAt = now,
            parts = state.rows.toList(),
            settings = state.settings,
            lastLayout = state.layoutState?.result,
            lastLayoutParts = state.layoutState?.partsSnapshot?.toList(),
        )

        viewModelScope.launch {
            repository.saveProject(project)
            val newBaseline = computeWorkspaceSignature(state.rows, state.settings, finalName)
            updateAndSave { s ->
                s.copy(
                    currentProjectId = projectId,
                    currentProjectName = finalName,
                    baselineSignature = newBaseline,
                )
            }
            showNotice("پروژهٔ «$finalName» ذخیره شد ✓")
        }
    }

    fun openProject(project: Project) {
        val parts = project.parts.ifEmpty { listOf(CutPart()) }
        val restoredLayoutState = project.lastLayout?.let { layout ->
            val snap = project.lastLayoutParts ?: parts
            LayoutState(
                result = layout,
                partsSnapshot = snap,
                signature = computeLayoutSignature(snap, settingsFromLayout(layout)),
            )
        }
        val activeId = (parts.find(::isEmptyPart) ?: parts.firstOrNull())?.id
        val baseline = computeWorkspaceSignature(parts, project.settings, project.name)
        updateAndSave { s ->
            s.copy(
                rows = parts,
                settings = project.settings,
                layoutState = restoredLayoutState,
                currentProjectId = project.id,
                currentProjectName = project.name,
                activePartId = activeId,
                baselineSignature = baseline,
                layoutError = null,
                checkedCodes = emptySet(),
            )
        }
        showNotice("پروژهٔ «${project.name}» باز شد")
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project.id)
            if (project.id == _uiState.value.currentProjectId) {
                updateAndSave { s ->
                    s.copy(
                        currentProjectId = null,
                        baselineSignature = "__unsaved__",
                    )
                }
            }
            showNotice("پروژهٔ «${project.name}» حذف شد")
        }
    }

    fun newProject() {
        val empty = listOf(CutPart())
        val freshSettings = DEFAULT_LAYOUT_SETTINGS
        val baseline = computeWorkspaceSignature(empty, freshSettings, UNTITLED_PROJECT_NAME)
        updateAndSave { s ->
            s.copy(
                rows = empty,
                settings = freshSettings,
                layoutState = null,
                currentProjectId = null,
                currentProjectName = UNTITLED_PROJECT_NAME,
                activePartId = empty.first().id,
                baselineSignature = baseline,
                layoutError = null,
                checkedCodes = emptySet(),
                activeStep = AppStep.SETUP,
            )
        }
        showNotice("پروژهٔ جدید شروع شد")
    }

    fun showNotice(message: String) {
        _uiState.update { it.copy(saveNotice = message) }
        viewModelScope.launch {
            delay(3200)
            _uiState.update { current ->
                if (current.saveNotice == message) current.copy(saveNotice = null) else current
            }
        }
    }
}
