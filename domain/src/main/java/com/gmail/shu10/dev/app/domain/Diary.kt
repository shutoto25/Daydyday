package com.gmail.shu10.dev.app.domain

/**
 * 日記データクラス
 */
data class Diary(
    /** ID（更新時にのみ使用）*/
    val id: Long? = null,
    /** yyyy-MM-dd形式の日付（検索用）*/
    val date: String,
    /** 内容 */
    var content: String
)