package com.gmail.shu10.dev.app.feature.home

data class DiaryUIModel(
    /** ID（更新時にのみ使用）*/
    val id: Long? = null,
    /** UUID(一意の識別子) */
    val uuid: String,
    /** yyyy-MM-dd形式の日付（検索用）*/
    val date: String,
    /** タイトル */
    val title: String,
    /** 内容 */
    val content: String,
    /** 写真 */
    val photoPath: String?,
    /** 動画 */
    val videoPath: String?
)