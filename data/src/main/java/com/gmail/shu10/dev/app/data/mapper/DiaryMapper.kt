package com.gmail.shu10.dev.app.data.mapper

import com.gmail.shu10.dev.app.data.database.DiaryEntity
import com.gmail.shu10.dev.app.domain.Diary

/**
 * entity -> domain
 */
fun DiaryEntity.toDomain(): Diary {
    return Diary(
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