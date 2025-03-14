package io.github.gmazzo.codeowners

import java.io.File
import kotlin.test.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodeOwnersTaskOutputsTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("collectMappings")
    fun `verify CodeOwnersTask output`(path: String, expected: File?, actual: File?) {
        assertEquals(
            expected?.readText(), actual?.readText(),
            "Generated mappings for '$path' do not match the expected one"
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("collectReports")
    fun `verify CodeOwnersReportTask output`(path: String, expected: File?, actual: File?) {
        assertEquals(
            expected?.readText(), actual?.readText(),
            "Generated report for '$path' do not match the expected one"
        )
    }

    private fun collectMappings() = collect("Mappings")

    private fun collectReports() = collect("Reports")

    private fun collect(type: String): Collection<Arguments> {
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

        collectFromResources("/expected$type") { acc, it -> acc[0] = it }
        collectFromResources("/actual$type") { acc, it -> acc[1] = it }
        return mappings.map { (path, files) -> arguments(path, *files) }
    }

}
