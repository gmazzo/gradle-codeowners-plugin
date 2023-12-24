package org.test.kotlin.app

import io.github.gmazzo.codeowners.codeOwners
import io.github.gmazzo.codeowners.codeOwnersOf
import org.test.kotlin.utils.AppUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppOwnersTest {

    @Test
    fun ownerOfApp() {
        assertEquals(setOf("app-devs"), codeOwnersOf<AppClass>())
    }

    @Test
    fun ownerOfAppUtils() {
        assertEquals(setOf("app-devs"), codeOwnersOf<AppClass>())
    }

    @Test
    fun ownerOfAppUtilsPackage() {
        assertEquals(setOf("app-devs"), codeOwnersOf<AppUtils>())
    }

    @Test
    fun ownerFromExceptionStacktrace() {
        val exception = assertFailsWith<AppException> { rethrow { AppException("myException") } }

        val expectedOwners = setOf(if (isJVM) "utils-devs" else "app-devs") // on JVM we have stackstraces

        assertEquals(expectedOwners, exception.codeOwners)
    }

}
