package com.gmail.shu10.dev.app.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 日記エンティティ
 */
@Entity(
    tableName = "diaries",
    indices = [Index(value = ["date"], unique = true)]
)
class DiaryEntity(
    /** UUID(一意の識別子) */
    @PrimaryKey val uuid: String,
    /** yyyy-MM-dd形式の日付（検索用）*/
    val date: String,
    /** タイトル */
    val title: String,
    /** 内容 */
    val content: String,
    /** 写真 */
    val photoPath: String?,
    /** 動画 */
    val videoPath: String?,
    /** 位置情報 */
    val location: String?,
    /** 同期済みフラグ */
    val isSynced: Boolean = false
)