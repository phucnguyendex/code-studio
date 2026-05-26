package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val githubUsername: String = "",
    val githubToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
