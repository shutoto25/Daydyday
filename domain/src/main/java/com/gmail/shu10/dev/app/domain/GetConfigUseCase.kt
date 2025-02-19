package com.gmail.shu10.dev.app.domain

import javax.inject.Inject

class GetConfigUseCase @Inject constructor(private val repository: ISharedPreferenceRepository) {
    fun getMediaType(): String {
       return repository.getString("mediaType")
    }
}
