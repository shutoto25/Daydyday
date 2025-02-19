package com.gmail.shu10.dev.app.domain

import javax.inject.Inject

class SetConfigUseCase @Inject constructor(private val repository: ISharedPreferenceRepository) {
    fun setMediaType(mediaType: String) {
        repository.saveString("mediaType", mediaType)
    }
}
