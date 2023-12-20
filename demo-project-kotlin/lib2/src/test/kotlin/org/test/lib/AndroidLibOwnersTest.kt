package org.test.lib

import io.github.gmazzo.codeowners.codeOwnersOf
import org.test.utils.AndroidLibUtils
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidLibOwnersTest {

    @Test
    fun ownerOfAndroidLib() {
        assertEquals(setOf("libs-devs"), codeOwnersOf<AndroidLibUtils>())
        assertEquals(setOf("libs-impl"), codeOwnersOf<AndroidLibUtils.Impl>())
    }

}
