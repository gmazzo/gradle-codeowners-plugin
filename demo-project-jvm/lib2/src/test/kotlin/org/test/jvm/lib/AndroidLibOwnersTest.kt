package org.test.jvm.lib

import io.github.gmazzo.codeowners.codeOwnersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import org.test.jvm.utils.AndroidLibUtils

class AndroidLibOwnersTest {

    @Test
    fun ownerOfSelf() {
        assertEquals(setOf("test-devs"), codeOwnersOf<AndroidLibOwnersTest>())
    }

    @Test
    fun ownerOfAndroidLib() {
        assertEquals(setOf("libs-devs"), codeOwnersOf<AndroidLibUtils>())
        assertEquals(setOf("libs-devs"), codeOwnersOf<AndroidLibUtils.Impl>())
    }

}
