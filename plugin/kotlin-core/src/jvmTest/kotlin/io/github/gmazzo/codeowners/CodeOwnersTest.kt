package io.github.gmazzo.codeowners

import org.test.AnotherBaz
import org.test.Bar
import org.test.BarImpl
import org.test.Baz
import org.test.Foo
import org.test.FooImpl
import org.test.baz
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals

// it only applies to JVM as annotations are not available on other targets
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
        assertEquals(setOf("bar-inner"), codeOwnersOf<BarImpl.Inner>())
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

        assertEquals(setOf("bar-inner"), exception?.codeOwners)
    }

    @Test
    fun `reports_proxy_of_Foo_owners_correctly`() {
        val proxy = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(Foo::class.java)) { _, _, _ -> }

        assertEquals(setOf("foo"), proxy::class.codeOwners)
    }

    @Test
    fun `reports_baz_function_owners_correctly`() {
        assertEquals(setOf("baz"), ::baz.codeOwners)
    }

}