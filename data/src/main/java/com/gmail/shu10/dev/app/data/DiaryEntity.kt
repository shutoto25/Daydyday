package com.gmail.shu10.dev.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 日記エンティティ
 */
@Entity(tableName = "diaries")
class DiaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val content: String
)