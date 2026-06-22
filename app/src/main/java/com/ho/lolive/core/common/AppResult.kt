package com.ho.lolive.core.common

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val throwable: Throwable, val message: String? = throwable.message) : AppResult<Nothing>()
    data object Loading : AppResult<Nothing>()
}
