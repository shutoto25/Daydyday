package com.gmail.shu10.dev.app.feature.home

import androidx.lifecycle.ViewModel
import com.gmail.shu10.dev.app.domain.SampleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val sampleUseCase: SampleUseCase) : ViewModel() {

    fun getStringFromSharedPreference(key: String): String {
        return sampleUseCase.getStringFromSharedPreference(key)
    }

    fun saveStringToSharedPreference(key: String, value: String) {
        sampleUseCase.saveStringToSharedPreference(key, value)
    }
}