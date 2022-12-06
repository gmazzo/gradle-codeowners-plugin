package com.github.gmazzo.codeowners

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
        assertEquals(listOf("foo"), codeOwnersOf<Foo>())
    }

    @Test
    fun `reports Baz owners correctly`() {
        assertEquals(listOf("baz"), codeOwnersOf<Baz>())
    }

    /**
     * TODO This is a known limitation test
     * Event both [Baz] and [AnotherBaz] comes from Baz.kt, only [Baz]'s only will be reported as there is no known way
     * to track the original file name from a [Class]
     */
    @Test
    fun `reports AnotherBaz owners correctly`() {
        assertEquals(null, codeOwnersOf<AnotherBaz>())
        Exception().fillInStackTrace()
    }

    @Test
    fun `reports FooImpl owners correctly`() {
        assertEquals(listOf("foo"), codeOwnersOf<FooImpl>())
    }

    @Test
    fun `reports FooBar owners correctly`() {
        assertEquals(listOf("foo", "bar"), codeOwnersOf<FooBar>())
    }

    @Test
    fun `reports FooBarImpl owners correctly`() {
        assertEquals(listOf("foo", "bar"), codeOwnersOf<FooBarImpl>())
    }

    @Test
    fun `reports Bar owners correctly`() {
        assertEquals(listOf("bar"), codeOwnersOf<Bar>())
    }

    @Test
    fun `reports BarImpl owners correctly`() {
        assertEquals(listOf("bar-impl"), codeOwnersOf<BarImpl>())
    }

    @Test
    fun `reports BarImpl$Inner owners correctly`() {
        assertEquals(listOf("bar-impl"), codeOwnersOf<BarImpl.Inner>())
    }

    @Test
    fun `given an unowned class, returns null`() {
        assertEquals(null, codeOwnersOf<CodeOwnersTest>())
    }

}
