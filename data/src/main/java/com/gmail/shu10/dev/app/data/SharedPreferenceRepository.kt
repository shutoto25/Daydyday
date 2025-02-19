package com.gmail.shu10.dev.app.data

import android.content.Context
import android.content.SharedPreferences
import com.gmail.shu10.dev.app.domain.ISharedPreferenceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * SharedPreference操作用repository
 */
class SharedPreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : ISharedPreferenceRepository {
    /**
     * SharedPreferenceインスタンス
     */
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("app_shared_preferences", Context.MODE_PRIVATE)
    }

    override fun saveString(key: String, value: String) {
        // 非同期に書き込むため apply() を使用
        sharedPreferences.edit().putString(key, value).apply()
    }

    override fun getString(key: String): String {
        // キーが存在しない場合は空文字列を返す
        return sharedPreferences.getString(key, "") ?: ""
    }
}