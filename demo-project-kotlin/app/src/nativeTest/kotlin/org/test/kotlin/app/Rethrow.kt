package org.test.kotlin.app

actual const val isJVM = false

actual fun rethrow(throwable: () -> Throwable) {
    throw throwable()
}
