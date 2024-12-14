package com.gmail.shu10.dev.app.data.mapper

import com.gmail.shu10.dev.app.data.database.DiaryEntity
import com.gmail.shu10.dev.app.domain.Diary

/**
 * entity -> domain
 */
fun DiaryEntity.toDomain(): Diary {
    return Diary(
        id = this.id,
        content = this.content,
        date = this.date
    )
}

/**
 * domain -> entity
 */
fun Diary.toEntity(): DiaryEntity {
    return DiaryEntity(
        id = this.id ?: 0, // idがnullの場合は新規作成（0を指定してautoGenerateに任せる）
        content = this.content,
        date = this.date
    )
}