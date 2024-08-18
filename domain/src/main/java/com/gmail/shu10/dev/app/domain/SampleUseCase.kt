package com.gmail.shu10.dev.app.domain

import javax.inject.Inject

class SampleUseCase @Inject constructor(private val sharedPreferenceRepo: ISharedPreferenceRepository) {

    fun getStringFromSharedPreference(key: String): String {
        return sharedPreferenceRepo.getString(key)
    }

    fun saveStringToSharedPreference(key: String, value: String) {
        sharedPreferenceRepo.saveString(key, value)
    }
}