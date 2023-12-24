package org.test.jvm.lib

import io.github.gmazzo.codeowners.codeOwners
import org.test.jvm.utils.LibUtils
import org.test.jvm.utils.more.MoreUtils
import kotlin.test.Test
import kotlin.test.assertEquals

class LibOwnersTest {

    @Test
    fun ownerOfSelf() {
        assertEquals(setOf("libs-devs"), LibOwnersTest::class.java.codeOwners)
    }

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
        assertEquals(setOf("utils-devs"), MoreUtils.Companion::class.java.codeOwners)
    }

}
