package org.test

import io.github.gmazzo.codeowners.CodeOwners

@CodeOwners("bar")
object BarImpl : Bar() {

    @CodeOwners("bar-inner")
    object Inner {
        fun throwException() {
            error("Bar")
        }
    }

}
