package com.gmail.shu10.dev.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _dataList = MutableStateFlow<List<String>>(emptyList())
    val dataList: StateFlow<List<String>> = _dataList

    init {
        fetchDateList()
    }

    private fun fetchDateList() {
        viewModelScope.launch {
            _dataList.value = getDateList()
        }
    }

    private fun getDateList(): List<String> {
        val dateFormat = SimpleDateFormat("yyyy-mm-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val dateList = mutableListOf<String>()

        for (i in 0 until 30) {
            dateList.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dateList
    }

//    fun getStringFromSharedPreference(key: String): String {
//        return sampleUseCase.getStringFromSharedPreference(key)
//    }
//
//    fun saveStringToSharedPreference(key: String, value: String) {
//        sampleUseCase.saveStringToSharedPreference(key, value)
//    }
}