package com.gmail.shu10.dev.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 日記エンティティ
 */
@Entity(tableName = "diaries")
class DiaryEntity(
    /** ID（内部的な一意性を保証）*/
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** yyyy-MM-dd形式の日付（検索用）*/
    val date: String,
    /** 内容 */
    val content: String
)