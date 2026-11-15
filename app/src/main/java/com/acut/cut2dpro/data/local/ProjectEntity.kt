package com.acut.cut2dpro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.acut.cut2dpro.domain.model.Project
import com.acut.cut2dpro.domain.model.cutPartsFromJsonString
import com.acut.cut2dpro.domain.model.layoutResultFromJson
import com.acut.cut2dpro.domain.model.layoutSettingsFromJson
import com.acut.cut2dpro.domain.model.toJson
import com.acut.cut2dpro.domain.model.toJsonString
import org.json.JSONObject

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val partsJson: String,
    val settingsJson: String,
    val lastLayoutJson: String?,
    val lastLayoutPartsJson: String?,
)

fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    partsJson = parts.toJsonString(),
    settingsJson = settings.toJson().toString(),
    lastLayoutJson = lastLayout?.toJson()?.toString(),
    lastLayoutPartsJson = lastLayoutParts?.toJsonString(),
)

fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    parts = runCatching { cutPartsFromJsonString(partsJson) }.getOrDefault(emptyList()),
    settings = runCatching { layoutSettingsFromJson(JSONObject(settingsJson)) }
        .getOrDefault(com.acut.cut2dpro.domain.model.DEFAULT_LAYOUT_SETTINGS),
    lastLayout = lastLayoutJson?.let { raw ->
        runCatching { layoutResultFromJson(JSONObject(raw)) }.getOrNull()
    },
    lastLayoutParts = lastLayoutPartsJson?.let { raw ->
        runCatching { cutPartsFromJsonString(raw) }.getOrNull()
    },
)
