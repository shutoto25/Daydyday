package com.gmail.shu10.dev.app.data.mapper

import com.gmail.shu10.dev.app.data.database.DiaryEntity
import com.gmail.shu10.dev.app.domain.Diary

/**
 * entity -> domain
 */
fun DiaryEntity.toDomain(): Diary {
    return Diary(
        id = this.id,
        uuid = this.uuid,
        title = this.title,
        content = this.content,
        photoPath = this.photoPath,
        videoPath = this.videoPath,
        location = this.location,
        date = this.date,
        isSynced = this.isSynced
    )
}

/**
 * domain -> entity
 */
fun Diary.toEntity(): DiaryEntity {
    return DiaryEntity(
        id = this.id ?: 0, // idがnullの場合は新規作成（0を指定してautoGenerateに任せる）
        uuid = this.uuid,
        title = this.title,
        content = this.content,
        photoPath = this.photoPath,
        videoPath = this.videoPath,
        location = this.location,
        date = this.date,
        isSynced = this.isSynced
    )
}