package io.github.gmazzo.codeowners

import bar.Bar
import bar.impl.BarImpl
import baz.AnotherBaz
import baz.Baz
import foo.Foo
import foo.bar.FooBar
import foo.bar.impl.FooBarImpl
import foo.impl.FooImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CodeOwnersTest {

    @Test
    fun `reports Foo owners correctly`() {
        assertEquals(setOf("foo"), codeOwnersOf<Foo>())
    }

    @Test
    fun `reports Baz owners correctly`() {
        assertEquals(setOf("baz"), codeOwnersOf<Baz>())
    }

    /**
     * TODO This is a known limitation test
     * Both [Baz] and [AnotherBaz] comes from `Baz.kt`, but only [Baz]'s will be reported as there is no known way
     * to track the original file name from a [Class].
     *
     * For those cases, you can use `Throwable.codeOwners` instead (next test)
     */
    @Test
    fun `reports AnotherBaz owners correctly`() {
        assertEquals(null, codeOwnersOf<AnotherBaz>())
    }

    @Test
    fun `reports AnotherBaz owners correctly, when reading from a stacktrace`() {
        val throwable = runCatching { AnotherBaz() }.exceptionOrNull()

        assertEquals(setOf("baz"), throwable?.codeOwners)
    }

    @Test
    fun `reports FooImpl owners correctly`() {
        assertEquals(setOf("foo"), codeOwnersOf<FooImpl>())
    }

    @Test
    fun `reports FooBar owners correctly`() {
        assertEquals(setOf("foo", "bar"), codeOwnersOf<FooBar>())
    }

    @Test
    fun `reports FooBarImpl owners correctly`() {
        assertEquals(setOf("foo", "bar"), codeOwnersOf<FooBarImpl>())
    }

    @Test
    fun `reports Bar owners correctly`() {
        assertEquals(setOf("bar"), codeOwnersOf<Bar>())
    }

    @Test
    fun `reports BarImpl owners correctly`() {
        assertEquals(setOf("bar-impl"), codeOwnersOf<BarImpl>())
    }

    @Test
    fun `reports BarImpl$Inner owners correctly`() {
        assertEquals(setOf("bar-impl"), codeOwnersOf<BarImpl.Inner>())
    }

    @Test
    fun `given an unowned class, returns null`() {
        assertEquals(null, codeOwnersOf<CodeOwnersTest>())
    }

}
