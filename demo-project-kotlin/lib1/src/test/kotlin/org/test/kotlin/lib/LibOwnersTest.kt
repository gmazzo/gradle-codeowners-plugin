package org.test.kotlin.lib

import io.github.gmazzo.codeowners.codeOwners
import org.test.kotlin.utils.LibUtils
import org.test.kotlin.utils.more.MoreUtils
import kotlin.test.Test
import kotlin.test.assertEquals

class LibOwnersTest {

    @Test
    fun ownerOfLib() {
        assertEquals(setOf("libs-devs"), LibClass::class.java.codeOwners)
    }

    @Test
    fun ownerOfUtils() {
        assertEquals(setOf("libs-devs"), LibUtils::class.java.codeOwners)
    }

    @Test
    fun ownerOfMoreUtils() {
        assertEquals(setOf("utils-devs"), MoreUtils::class.java.codeOwners)
        assertEquals(setOf("utils-more"), MoreUtils.Companion::class.java.codeOwners)
    }

}
