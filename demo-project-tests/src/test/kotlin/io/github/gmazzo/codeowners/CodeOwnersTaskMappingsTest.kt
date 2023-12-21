package io.github.gmazzo.codeowners

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodeOwnersTaskMappingsTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("mappings")
    fun `verify generated mappings matches the expected one`(path: String, expected: File?, actual: File?) {
        assertEquals(expected?.readText(), actual?.readText(),
            "Generated mappings for '$path' do not match the expected one")
    }

    private fun mappings(): Collection<Arguments> {
        val mappings = sortedMapOf<String, Array<File?>>()

        fun collectFromResources(baseResourceDir: String, mapper: (acc: Array<File?>, it: File) -> Unit) {
            val root = File(checkNotNull(javaClass.getResource(baseResourceDir)) {
                "'$baseResourceDir' not found in resources"
            }.file)

            root.walkTopDown().forEach {
                if (it.isFile) {
                    val path = it.relativeTo(root).path

                    mapper(mappings.getOrPut(path) { arrayOfNulls(2) }, it)
                }
            }
        }

        collectFromResources("/expectedMappings") { acc, it -> acc[0] = it }
        collectFromResources("/actualMappings") { acc, it -> acc[1] = it }
        return mappings.map { (path, files) -> arguments(path, *files) }
    }

}
