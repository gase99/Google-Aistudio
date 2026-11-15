package com.acut.cut2dpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acut.cut2dpro.ui.AppStep
import com.acut.cut2dpro.ui.MainUiState
import com.acut.cut2dpro.ui.MainViewModel
import com.acut.cut2dpro.ui.components.AnalysisScreen
import com.acut.cut2dpro.ui.components.ExportsScreen
import com.acut.cut2dpro.ui.components.LabelsScreen
import com.acut.cut2dpro.ui.components.SetupScreen
import com.acut.cut2dpro.ui.components.TopHeaderBar
import com.acut.cut2dpro.ui.theme.ACut2DProTheme
import com.acut.cut2dpro.ui.theme.AmberGroove
import com.acut.cut2dpro.ui.theme.AppBackground
import com.acut.cut2dpro.ui.theme.SkyPrimaryDark
import com.acut.cut2dpro.ui.theme.SkySoft
import com.acut.cut2dpro.ui.theme.SkySoftBorder
import com.acut.cut2dpro.ui.theme.Slate200
import com.acut.cut2dpro.ui.theme.Slate400
import com.acut.cut2dpro.ui.theme.Slate500
import com.acut.cut2dpro.ui.theme.Slate700
import com.acut.cut2dpro.ui.theme.Slate900
import com.acut.cut2dpro.ui.theme.SubtleSurface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ACut2DProTheme {
                val viewModel: MainViewModel = viewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val projects by viewModel.projects.collectAsStateWithLifecycle()

                ACutAppShell(
                    state = state,
                    projects = projects,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun ACutAppShell(
    state: MainUiState,
    projects: List<com.acut.cut2dpro.domain.model.Project>,
    viewModel: MainViewModel,
) {
    BackHandler(enabled = state.activeStep != AppStep.SETUP) {
        viewModel.setStep(AppStep.SETUP)
    }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White),
            ) {
                TopHeaderBar(
                    currentProjectId = state.currentProjectId,
                    currentProjectName = state.currentProjectName,
                    isDirty = state.isDirty,
                    saveNotice = state.saveNotice,
                    projects = projects,
                    onSaveDirect = { viewModel.saveProjectWithName(null, saveAs = false) },
                    onSaveWithName = { name, saveAs -> viewModel.saveProjectWithName(name, saveAs = saveAs) },
                    onNewProject = viewModel::newProject,
                    onOpenProject = viewModel::openProject,
                    onDeleteProject = viewModel::deleteProject,
                )
                StepNavigationStrip(
                    activeStep = state.activeStep,
                    hasLayout = state.layoutState != null,
                    isStale = state.isStale,
                    onSelectStep = viewModel::setStep,
                )
            }
        },
    ) { innerPadding ->
        if (!state.ready) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = SkyPrimaryDark)
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1040.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (state.activeStep) {
                    AppStep.SETUP -> SetupScreen(
                        state = state,
                        onUpdateSettings = viewModel::updateSettings,
                        onCalculate = { viewModel.calculate(navigateToAnalysis = true) },
                        onSetActivePart = viewModel::setActivePart,
                        onClearFocusTarget = viewModel::clearFocusTarget,
                        onUpdatePart = viewModel::updatePart,
                        onInsertRowAfter = { afterId, dup -> viewModel.insertRowAfter(afterId, dup) },
                        onDeleteRow = viewModel::deleteRow,
                        onLoadSample = viewModel::loadSampleParts,
                        onRemoveEmpty = viewModel::removeEmptyRows,
                        onClearAll = viewModel::clearAllRows,
                    )
                    AppStep.ANALYSIS -> AnalysisScreen(
                        state = state,
                        onRecalculate = { viewModel.calculate(navigateToAnalysis = false) },
                        onGoToSetup = { viewModel.setStep(AppStep.SETUP) },
                        onZoomChange = viewModel::setZoom,
                        onToggleChecklistCode = viewModel::toggleChecklistCode,
                        onShowNotice = viewModel::showNotice,
                    )
                    AppStep.LABELS -> LabelsScreen(
                        state = state,
                        onRecalculate = { viewModel.calculate(navigateToAnalysis = false) },
                        onGoToSetup = { viewModel.setStep(AppStep.SETUP) },
                    )
                    AppStep.EXPORTS -> ExportsScreen(
                        state = state,
                        onRecalculate = { viewModel.calculate(navigateToAnalysis = false) },
                        onGoToSetup = { viewModel.setStep(AppStep.SETUP) },
                        onShowNotice = viewModel::showNotice,
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "A-Cut 2Dpro — چیدمان بهینهٔ دو‌بعدی برش ورق، نقشه، چک‌لیست تولید، لیبل و خروجی DXF/PDF",
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate400,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StepNavigationStrip(
    activeStep: AppStep,
    hasLayout: Boolean,
    isStale: Boolean,
    onSelectStep: (AppStep) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SubtleSurface,
        border = BorderStroke(1.dp, Slate200),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppStep.entries.forEach { step ->
                val selected = activeStep == step
                val showStaleDot = hasLayout && isStale && step != AppStep.SETUP

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectStep(step) }
                        .testTag("step_tab_${step.name.lowercase()}"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) SkySoft else Color.White,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) SkySoftBorder else Slate200,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (selected) SkyPrimaryDark else Slate200),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = step.numberFa,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else Slate700,
                            )
                        }
                        Text(
                            text = step.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Slate900 else Slate500,
                        )
                        if (showStaleDot) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AmberGroove),
                            )
                        }
                    }
                }
            }
        }
    }
}
