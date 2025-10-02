package org.test.kotlin.lib

import io.github.gmazzo.codeowners.codeOwners
import kotlin.test.Test
import kotlin.test.assertEquals
import org.test.kotlin.utils.LibUtils
import org.test.kotlin.utils.more.MoreUtils

class LibOwnersTest {

    @Test
    fun ownerOfLib() {
        assertEquals(setOf("kt-libs-devs"), LibClass::class.java.codeOwners)
    }

    @Test
    fun ownerOfUtils() {
        assertEquals(setOf("kt-libs-devs"), LibUtils::class.java.codeOwners)
    }

    @Test
    fun ownerOfMoreUtils() {
        assertEquals(setOf("kt-utils-devs"), MoreUtils::class.java.codeOwners)
        assertEquals(setOf("kt-utils-devs"), MoreUtils.Companion::class.java.codeOwners)
    }

}
