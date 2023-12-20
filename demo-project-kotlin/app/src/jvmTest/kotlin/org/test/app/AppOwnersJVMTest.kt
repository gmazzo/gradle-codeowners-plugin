package org.test.app

import io.github.gmazzo.codeowners.codeOwners
import io.github.gmazzo.codeowners.codeOwnersOf
import org.test.lib.LibClass
import org.test.utils.more.MoreUtils
import org.test.utils.rethrow
import kotlin.test.Test
import kotlin.test.assertEquals

class AppOwnersJVMTest {

    @Test
    fun ownerOfLib() {
        assertEquals(setOf("libs-devs"), codeOwnersOf<LibClass>())
    }

    @Test
    fun ownerOfLibUtils() {
        assertEquals(setOf("libs-devs"), codeOwnersOf<LibClass>())
    }

    @Test
    fun ownerOfMoreUtils() {
        assertEquals(setOf("utils-devs"), codeOwnersOf<MoreUtils>())
    }

    @Test
    fun ownerOfUtilFunctions() {
        assertEquals(setOf("utils-devs"), ::rethrow.codeOwners)
    }

}
