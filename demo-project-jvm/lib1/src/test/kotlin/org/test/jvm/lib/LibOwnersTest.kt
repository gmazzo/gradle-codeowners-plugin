package org.test.jvm.lib

import io.github.gmazzo.codeowners.codeOwners
import kotlin.test.Test
import kotlin.test.assertEquals
import org.test.jvm.utils.LibUtils
import org.test.jvm.utils.more.MoreUtils

class LibOwnersTest {

    @Test
    fun ownerOfSelf() {
        assertEquals(setOf("jvm-libs-devs"), LibOwnersTest::class.java.codeOwners)
    }

    @Test
    fun ownerOfLib() {
        assertEquals(setOf("jvm-libs-devs"), LibClass::class.java.codeOwners)
    }

    @Test
    fun ownerOfUtils() {
        assertEquals(setOf("jvm-libs-devs"), LibUtils::class.java.codeOwners)
    }

    @Test
    fun ownerOfMoreUtils() {
        assertEquals(setOf("jvm-utils-devs"), MoreUtils::class.java.codeOwners)
        assertEquals(setOf("jvm-utils-devs"), MoreUtils.Companion::class.java.codeOwners)
    }

}
