package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val path: String,
    val isFolder: Boolean,
    val content: String = "",
    val language: String = "plaintext",
    val lastModified: Long = System.currentTimeMillis()
)
