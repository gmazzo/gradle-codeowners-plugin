package org.test.app

actual const val isJVM = true

actual fun rethrow(throwable: () -> Throwable) =
    org.test.utils.rethrow(throwable)
