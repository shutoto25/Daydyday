package com.gmail.shu10.dev.app.domain

/**
 * SharedPreference操作用repository interface
 */
interface ISharedPreferenceRepository {
    /**
     * 文字列を保存する
     * @param key 保存するキー
     * @param value 保存する値
     */
    fun saveString(key: String, value: String)

    /**
     * 文字列を取得する
     * @param key 取得するキー
     */
    fun getString(key: String): String
}