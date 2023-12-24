package org.test.kotlin.utils

fun rethrow(throwable: () -> Throwable) {
    throw throwable()
}
