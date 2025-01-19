package com.gmail.shu10.dev.app.domain

import kotlinx.serialization.Serializable

/**
 * 日記データクラス
 */
 @Serializable
data class Diary(
    /** UUID(一意の識別子) */
    val uuid: String = "",
    /** yyyy-MM-dd形式の日付（検索用）*/
    val date: String,
    /** タイトル */
    val title: String = "",
    /** 内容 */
    val content: String = "",
    /** 写真 */
    val photoPath: String? = null,
    /** 動画 */
    val videoPath: String? = null,
    /** 1秒動画 */
    val trimmedVideoPath: String? = null,
    /** 位置情報 */
    val location: String? = null,
    /** 同期済みフラグ */
    val isSynced: Boolean = false
)