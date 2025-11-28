package com.nexusbiz.nexusbiz.util

/**
 * Extensiones para Result<T> que permiten usar onSuccess y onFailure
 * de manera similar a otros lenguajes funcionales
 */
inline fun <T> Result<T>.onSuccess(action: (value: T) -> Unit): Result<T> {
    if (isSuccess) {
        action(getOrNull()!!)
    }
    return this
}

inline fun <T> Result<T>.onFailure(action: (exception: Throwable) -> Unit): Result<T> {
    if (isFailure) {
        action(exceptionOrNull()!!)
    }
    return this
}

