package org.test.kotlin.app

class AppException(message: String) : RuntimeException(message)

expect val isJVM: Boolean

expect fun rethrow(throwable: () -> Throwable)
