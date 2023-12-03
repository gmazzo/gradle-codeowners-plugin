package org.test.utils

fun rethrow(throwable: () -> Throwable) {
    throw throwable()
}
