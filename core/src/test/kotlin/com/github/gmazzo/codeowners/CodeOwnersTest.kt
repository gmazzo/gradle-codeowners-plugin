package com.github.gmazzo.codeowners

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class CodeOwnersTest {

    @ParameterizedTest
    @CsvSource(
        "foo.Foo,                   foo",
        "foo.impl.FooImpl,          foo",
        "foo.bar.FooBar,            foo|bar",
        "foo.bar.impl.FooBarImpl,   foo|bar",
        "bar.Bar,                   bar",
        "bar.impl.BarImpl,          bar",
    )
    fun `given a class, reports a its owners correctly`(className: String, expectedCodeOwners: String) {
        val owners = Class.forName(className).codeOwner?.asList()

        assertEquals(expectedCodeOwners.split('|'), owners)
    }

    @Test
    fun `given an unowned class, returns null`() {
        assertEquals(null, CodeOwnersTest::class.codeOwner)
    }

}
