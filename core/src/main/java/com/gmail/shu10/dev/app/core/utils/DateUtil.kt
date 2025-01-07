package com.gmail.shu10.dev.app.core.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 日付フォーマット変換（yyyy-MM-dd -> yyyy/MM/dd）
 * @param date 日付（yyyy-MM-dd）
 * @return 日付（yyyy/MM/dd）
 */
fun convertDateFormat(date: String): String {
    val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val outputFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    val parsedDate = LocalDate.parse(date, inputFormatter)
    return parsedDate.format(outputFormatter)
}

/**
 * 日付から曜日を取得
 * @param date 日付（yyyy-MM-dd）
 * @return 曜日
 */
fun getDayOfWeek(date: String): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val parsedDate = LocalDate.parse(date, formatter)
    return parsedDate.dayOfWeek.toString()
}