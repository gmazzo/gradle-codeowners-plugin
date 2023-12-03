package io.github.gmazzo.codeowners

import org.test.*
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeOwnersTest {

    @Test
    fun reports_Foo_owners_correctly() {
        assertEquals(setOf("foo"), codeOwnersOf<Foo>())
        assertEquals(setOf("foo"), codeOwnersOf<FooImpl>())
    }

    @Test
    fun reports_Bar_owners_correctly() {
        assertEquals(setOf("bar"), codeOwnersOf<Bar>())
        assertEquals(setOf("bar"), BarImpl::class.codeOwners)
        assertEquals(setOf("bar"), codeOwnersOf<BarImpl.Inner>())
    }

    @Test
    fun reports_Baz_owners_correctly() {
        assertEquals(setOf("baz"), codeOwnersOf<Baz>())
        assertEquals(setOf("baz"), AnotherBaz::class.codeOwners)
    }

    @Test
    fun reports_owners_of_from_stackstrace_correctly() {
        fun myFun() = BarImpl.Inner.throwException()

        val exception = runCatching { myFun() }.exceptionOrNull()

        assertEquals(setOf("bar"), exception?.codeOwners)
    }

}