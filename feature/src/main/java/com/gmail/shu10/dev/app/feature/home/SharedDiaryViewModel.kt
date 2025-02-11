package com.gmail.shu10.dev.app.feature.home

import androidx.lifecycle.ViewModel
import com.gmail.shu10.dev.app.domain.GetDiaryUseCase
import com.gmail.shu10.dev.app.domain.SaveDiaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ホーム画面と詳細画面で共有するViewModel
 */
@HiltViewModel
class SharedDiaryViewModel@Inject constructor(
    private val getDiaryUseCase: GetDiaryUseCase,
    private val saveDiaryUseCase: SaveDiaryUseCase
) : ViewModel() {


}