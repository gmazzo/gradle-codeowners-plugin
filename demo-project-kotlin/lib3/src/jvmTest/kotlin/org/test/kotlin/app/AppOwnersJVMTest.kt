package org.test.kotlin.app

import io.github.gmazzo.codeowners.codeOwners
import io.github.gmazzo.codeowners.codeOwnersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import org.test.kotlin.lib.LibClass
import org.test.kotlin.utils.more.MoreUtils
import org.test.kotlin.utils.rethrow

class AppOwnersJVMTest {

    @Test
    fun ownerOfLib() {
        assertEquals(setOf("kt-libs-devs"), codeOwnersOf<LibClass>())
    }

    @Test
    fun ownerOfLibUtils() {
        assertEquals(setOf("kt-libs-devs"), codeOwnersOf<LibClass>())
    }

    @Test
    fun ownerOfMoreUtils() {
        assertEquals(setOf("kt-utils-devs"), codeOwnersOf<MoreUtils>())
    }

    @Test
    fun ownerOfUtilFunctions() {
        assertEquals(setOf("kt-utils-devs"), ::rethrow.codeOwners)
    }

}
