package com.gmail.shu10.dev.app.feature.home

import androidx.lifecycle.ViewModel
import com.gmail.shu10.dev.app.domain.Diary
import com.gmail.shu10.dev.app.domain.GetDiaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(private val getDiaryUseCase: GetDiaryUseCase) : ViewModel() {

//     suspend fun getDiaryById(id: String?): Flow<Diary?> {
//        return if (id == null) {
//            flowOf(null)
//        } else {
//            getDiaryUseCase(id)
//        }
//    }

}