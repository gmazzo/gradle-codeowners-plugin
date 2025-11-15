package io.github.gmazzo.codeowners

import kotlin.test.Test
import kotlin.test.assertEquals
import org.test.AnotherBaz
import org.test.Bar
import org.test.BarImpl
import org.test.Baz
import org.test.Foo
import org.test.FooImpl

// it only applies to JVM as annotations are not available on other targets with the compiler plugin
open class CodeOwnersTest {

    @Test
    fun reports_Foo_owners_correctly() {
        assertEquals(setOf("foo").takeIf { hasAnnotationSupport }, codeOwnersOf<Foo>())
        assertEquals(setOf("foo").takeIf { hasAnnotationSupport }, codeOwnersOf<FooImpl>())
    }

    @Test
    fun reports_Bar_owners_correctly() {
        assertEquals(setOf("bar").takeIf { hasAnnotationSupport }, codeOwnersOf<Bar>())
        assertEquals(setOf("bar").takeIf { hasAnnotationSupport }, BarImpl::class.codeOwners)
        assertEquals(setOf("bar-inner").takeIf { hasAnnotationSupport }, codeOwnersOf<BarImpl.Inner>())
    }

    @Test
    fun reports_Baz_owners_correctly() {
        assertEquals(setOf("baz").takeIf { hasAnnotationSupport }, codeOwnersOf<Baz>())
        assertEquals(setOf("baz").takeIf { hasAnnotationSupport }, AnotherBaz::class.codeOwners)
    }

    @Test
    fun reports_owners_of_from_stackstrace_correctly() {
        fun myFun() = BarImpl.Inner.throwException()

        val exception = runCatching { myFun() }.exceptionOrNull()

        assertEquals(setOf("bar-inner").takeIf { hasAnnotationSupport }, exception?.codeOwners)
    }

}
