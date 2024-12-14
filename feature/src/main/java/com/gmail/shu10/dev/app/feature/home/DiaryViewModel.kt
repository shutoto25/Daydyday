package com.gmail.shu10.dev.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.domain.GetDiaryUseCase
import com.gmail.shu10.dev.app.domain.SaveDiaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val getDiaryUseCase: GetDiaryUseCase,
    private val saveDiaryUseCase: SaveDiaryUseCase
) : ViewModel() {

     suspend fun getDiaryById(id: String?): Flow<Diary?> {
        return if (id == null) {
            flowOf(null)
        } else {
            getDiaryUseCase(id)
        }
    }

    fun saveDiary(diary: Diary) {
        viewModelScope.launch {
            saveDiaryUseCase(diary)
        }
    }

}