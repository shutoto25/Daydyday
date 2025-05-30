package com.gmail.shu10.dev.app.core.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 日付フォーマット定数
 */
object DateFormatConstants {
    const val YYYY_MM_DD_SLASH = "yyyy/MM/dd"
    const val YYYY_MM_DD_HYPHEN = "yyyy-MM-dd"
}

/**
 * 今日の日付を指定されたフォーマットで取得
 * @param pattern 日付フォーマットパターン
 * @return フォーマットされた日付文字列
 */
fun getToday(pattern: String): String {
    val date = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return date.format(formatter)
}

/**
 * 日付フォーマット変換（yyyy-MM-dd -> yyyy/MM/dd）
 * @param date 日付（yyyy-MM-dd）
 * @return 日付（yyyy/MM/dd）
 */
fun convertDateFormat(date: String): String {
    val inputFormatter = DateTimeFormatter.ofPattern(DateFormatConstants.YYYY_MM_DD_HYPHEN)
    val outputFormatter = DateTimeFormatter.ofPattern(DateFormatConstants.YYYY_MM_DD_SLASH)
    val parsedDate = LocalDate.parse(date, inputFormatter)
    return parsedDate.format(outputFormatter)
}

/**
 * 日付から曜日を取得
 * @param date 日付（yyyy-MM-dd）
 * @return 曜日
 */
fun getDayOfWeek(date: String): String {
    val formatter = DateTimeFormatter.ofPattern(DateFormatConstants.YYYY_MM_DD_HYPHEN)
    val parsedDate = LocalDate.parse(date, formatter)
    return parsedDate.dayOfWeek.toString()
}

/**
 * 日付から年を取得
 * @param date 日付（yyyy-MM-dd）
 * @return 年
 */
fun getYear(date: String): Int {
    val formatter = DateTimeFormatter.ofPattern(DateFormatConstants.YYYY_MM_DD_HYPHEN)
    val parsedDate = LocalDate.parse(date, formatter)
    return parsedDate.year
}

/**
 * 日付から月を取得
 * @param date 日付（yyyy-MM-dd）
 * @return 月
 */
fun getMonth(date: String): Int {
    val formatter = DateTimeFormatter.ofPattern(DateFormatConstants.YYYY_MM_DD_HYPHEN)
    val parsedDate = LocalDate.parse(date, formatter)
    return parsedDate.monthValue
}

/**
 * 日付から月（文字列）を取得
 * @param date 日付（yyyy-MM-dd）
 * @return 月
 */
fun getMonthName(date: String): String {
    val formatter = DateTimeFormatter.ofPattern(DateFormatConstants.YYYY_MM_DD_HYPHEN)
    val parsedDate = LocalDate.parse(date, formatter)
    return parsedDate.month.toString()
}

/**
 * 日付から日を取得
 * @param date 日付（yyyy-MM-dd）
 * @return 日
 */
fun getDay(date: String): Int {
    val formatter = DateTimeFormatter.ofPattern(DateFormatConstants.YYYY_MM_DD_HYPHEN)
    val parsedDate = LocalDate.parse(date, formatter)
    return parsedDate.dayOfMonth
}