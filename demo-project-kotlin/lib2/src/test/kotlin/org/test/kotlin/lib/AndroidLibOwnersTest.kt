package org.test.kotlin.lib

import io.github.gmazzo.codeowners.codeOwnersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import org.test.kotlin.utils.AndroidLibUtils

class AndroidLibOwnersTest {

    @Test
    fun ownerOfAndroidLib() {
        assertEquals(setOf("kt-libs-devs"), codeOwnersOf<AndroidLibUtils>())
        assertEquals(setOf("kt-libs-devs"), codeOwnersOf<AndroidLibUtils.Impl>())
    }

}
