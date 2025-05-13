package com.gmail.shu10.dev.app.data.mapper

import com.gmail.shu10.dev.app.data.database.DiaryEntity
import com.gmail.shu10.dev.app.domain.Diary

/**
 * DiaryEntityをDiaryドメインモデルに変換
 * @return 変換されたDiaryドメインモデル
 */
fun DiaryEntity.toDomain(): Diary {
    return Diary(
        uuid = this.uuid,
        title = this.title,
        content = this.content,
        photoPath = this.photoPath,
        videoPath = this.videoPath,
        trimmedVideoPath = this.trimmedVideoPath,
        location = this.location,
        date = this.date,
        isSynced = this.isSynced
    )
}

/**
 * DiaryドメインモデルをDiaryEntityに変換
 * @return 変換されたDiaryEntity
 */
fun Diary.toEntity(): DiaryEntity {
    return DiaryEntity(
        uuid = this.uuid,
        title = this.title,
        content = this.content,
        photoPath = this.photoPath,
        videoPath = this.videoPath,
        trimmedVideoPath = this.trimmedVideoPath,
        location = this.location,
        date = this.date,
        isSynced = this.isSynced
    )
}