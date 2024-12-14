package com.gmail.shu10.dev.app.domain

import javax.annotation.Nullable

data class Diary(
    val id: Long,
    val content: String,
    val date: String
)