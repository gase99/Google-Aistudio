package com.acut.cut2dpro.data.repository

import com.acut.cut2dpro.data.local.ProjectDao
import com.acut.cut2dpro.data.local.WorkspaceDraft
import com.acut.cut2dpro.data.local.WorkspaceDraftStore
import com.acut.cut2dpro.data.local.toDomain
import com.acut.cut2dpro.data.local.toEntity
import com.acut.cut2dpro.domain.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val draftStore: WorkspaceDraftStore,
) {
    val allProjects: Flow<List<Project>> = projectDao.observeAllProjects().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun saveProject(project: Project) {
        withContext(Dispatchers.IO) {
            projectDao.upsertProject(project.toEntity())
        }
    }

    suspend fun deleteProject(id: String) {
        withContext(Dispatchers.IO) {
            projectDao.deleteProjectById(id)
        }
    }

    suspend fun loadDraft(): WorkspaceDraft = withContext(Dispatchers.IO) {
        draftStore.load()
    }

    suspend fun saveDraft(draft: WorkspaceDraft) = withContext(Dispatchers.IO) {
        draftStore.save(draft)
    }
}
