package org.test.utils

import io.github.gmazzo.codeowners.CodeOwners

sealed class AndroidLibUtils {

    // manual override owners
    @CodeOwners("libs-impl")
    data object Impl : AndroidLibUtils()

}
