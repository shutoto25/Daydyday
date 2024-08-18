package com.gmail.shu10.dev.app.data

import com.gmail.shu10.dev.app.domain.ISharedPreferenceRepository
import javax.inject.Inject

/**
 * SharedPreference操作用repository

 */
class SharedPreferenceRepository @Inject constructor() : ISharedPreferenceRepository {

    override fun saveString(key: String, value: String) {
        TODO("Not yet implemented")
    }

    override fun getString(key: String): String {
        TODO("Not yet implemented")
    }
}