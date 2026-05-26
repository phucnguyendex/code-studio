package com.example.database

import kotlinx.coroutines.flow.Flow

class EditorRepository(private val dao: WorkspaceDao) {
    val allProjects: Flow<List<ProjectEntity>> = dao.getAllProjects()
    val allSnippets: Flow<List<SnippetEntity>> = dao.getAllSnippets()

    fun getFilesForProject(projectId: Long): Flow<List<FileEntity>> {
        return dao.getFilesForProject(projectId)
    }

    suspend fun getFileById(fileId: Long): FileEntity? {
        return dao.getFileById(fileId)
    }

    suspend fun insertProject(project: ProjectEntity): Long {
        return dao.insertProject(project)
    }

    suspend fun updateProject(project: ProjectEntity) {
        dao.updateProject(project)
    }

    suspend fun deleteProject(projectId: Long) {
        dao.deleteFilesForProject(projectId)
        dao.deleteProject(projectId)
    }

    suspend fun insertFile(file: FileEntity): Long {
        return dao.insertFile(file)
    }

    suspend fun updateFile(file: FileEntity) {
        dao.updateFile(file)
    }

    suspend fun deleteFile(fileId: Long) {
        dao.deleteFile(fileId)
    }

    suspend fun insertSnippet(snippet: SnippetEntity): Long {
        return dao.insertSnippet(snippet)
    }

    suspend fun deleteSnippet(id: Long) {
        dao.deleteSnippet(id)
    }
}
