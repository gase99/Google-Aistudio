package com.acut.cut2dpro.data.local

import android.content.Context
import com.acut.cut2dpro.domain.model.CutPart
import com.acut.cut2dpro.domain.model.DEFAULT_LAYOUT_SETTINGS
import com.acut.cut2dpro.domain.model.LayoutSettings
import com.acut.cut2dpro.domain.model.LayoutState
import com.acut.cut2dpro.domain.model.UNTITLED_PROJECT_NAME
import com.acut.cut2dpro.domain.model.computeWorkspaceSignature
import com.acut.cut2dpro.domain.model.createSampleParts
import com.acut.cut2dpro.domain.model.cutPartFromJson
import com.acut.cut2dpro.domain.model.layoutResultFromJson
import com.acut.cut2dpro.domain.model.layoutSettingsFromJson
import com.acut.cut2dpro.domain.model.toJson
import org.json.JSONArray
import org.json.JSONObject

data class WorkspaceDraft(
    val projectId: String?,
    val projectName: String,
    val parts: List<CutPart>,
    val settings: LayoutSettings,
    val layoutState: LayoutState?,
    val baselineSignature: String,
)

class WorkspaceDraftStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): WorkspaceDraft {
        val raw = prefs.getString(KEY_DRAFT, null)
        if (raw != null) {
            runCatching {
                val obj = JSONObject(raw)
                val partsArr = obj.getJSONArray("parts")
                val parts = List(partsArr.length()) { idx -> cutPartFromJson(partsArr.getJSONObject(idx)) }
                if (parts.isNotEmpty()) {
                    val settings = layoutSettingsFromJson(obj.getJSONObject("settings"))
                    val projectId = obj.optString("projectId", "").takeIf { it.isNotEmpty() }
                    val projectName = obj.optString("projectName", UNTITLED_PROJECT_NAME)
                        .ifBlank { UNTITLED_PROJECT_NAME }
                    val layoutStateObj = obj.optJSONObject("layoutState")
                    val layoutState = layoutStateObj?.let { ls ->
                        val res = layoutResultFromJson(ls.getJSONObject("result"))
                        val snapArr = ls.getJSONArray("partsSnapshot")
                        val snap = List(snapArr.length()) { idx -> cutPartFromJson(snapArr.getJSONObject(idx)) }
                        val sig = ls.getString("signature")
                        LayoutState(result = res, partsSnapshot = snap, signature = sig)
                    }
                    val baseline = obj.optString("baselineSignature", "").ifEmpty {
                        computeWorkspaceSignature(parts, settings, projectName)
                    }
                    return WorkspaceDraft(
                        projectId = projectId,
                        projectName = projectName,
                        parts = parts,
                        settings = settings,
                        layoutState = layoutState,
                        baselineSignature = baseline,
                    )
                }
            }
        }
        val sample = createSampleParts()
        val settings = DEFAULT_LAYOUT_SETTINGS
        return WorkspaceDraft(
            projectId = null,
            projectName = UNTITLED_PROJECT_NAME,
            parts = sample,
            settings = settings,
            layoutState = null,
            baselineSignature = computeWorkspaceSignature(sample, settings, UNTITLED_PROJECT_NAME),
        )
    }

    fun save(draft: WorkspaceDraft) {
        runCatching {
            val root = JSONObject().apply {
                put("projectId", draft.projectId ?: "")
                put("projectName", draft.projectName)
                put("parts", JSONArray().apply { draft.parts.forEach { put(it.toJson()) } })
                put("settings", draft.settings.toJson())
                put(
                    "layoutState",
                    draft.layoutState?.let { ls ->
                        JSONObject().apply {
                            put("result", ls.result.toJson())
                            put("partsSnapshot", JSONArray().apply { ls.partsSnapshot.forEach { put(it.toJson()) } })
                            put("signature", ls.signature)
                        }
                    } ?: JSONObject.NULL,
                )
                put("baselineSignature", draft.baselineSignature)
            }
            prefs.edit().putString(KEY_DRAFT, root.toString()).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "acut_draft_prefs"
        private const val KEY_DRAFT = "acut_draft"
    }
}
