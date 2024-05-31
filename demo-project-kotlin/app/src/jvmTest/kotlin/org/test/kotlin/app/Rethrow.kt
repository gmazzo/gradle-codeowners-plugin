package org.test.kotlin.app

actual const val isJVM = true

actual fun rethrow(throwable: Throwable) =
    org.test.kotlin.utils.rethrow(throwable)
