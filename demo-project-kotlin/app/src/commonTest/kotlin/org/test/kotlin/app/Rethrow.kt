@file:JvmName("RethrowUtils")

package org.test.kotlin.app

import kotlin.jvm.JvmName

class AppException : RuntimeException("anException")

expect val isJVM: Boolean

expect fun rethrow(throwable: Throwable)
