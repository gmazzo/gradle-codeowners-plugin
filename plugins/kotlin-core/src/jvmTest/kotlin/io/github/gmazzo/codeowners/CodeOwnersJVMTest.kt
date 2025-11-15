package io.github.gmazzo.codeowners

import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import org.test.Foo
import org.test.baz

class CodeOwnersJVMTest : CodeOwnersTest() {

    @Test
    fun reports_proxy_of_Foo_owners_correctly() {
        val proxy = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(Foo::class.java)) { _, _, _ -> }

        assertEquals(setOf("foo"), proxy::class.codeOwners)
    }

    @Test
    fun reports_baz_function_owners_correctly() {
        assertEquals(setOf("baz"), ::baz.codeOwners)
    }

}
