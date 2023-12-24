package io.github.gmazzo.codeowners

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class CodeOwnersKotlinPluginCompatTest : CodeOwnersBaseCompatTest("io.github.gmazzo.codeowners.kotlin") {

    // this is a Kotlin plugin, we exclude non-Kotlin scenarios
    @ParameterizedTest(name = "{0}")
    @EnumSource(Kind::class, mode = EnumSource.Mode.EXCLUDE, names = ["alone", "withAndroid"])
    override fun `plugin can be applied with given classpath`(kind: Kind) {
        super.`plugin can be applied with given classpath`(kind)
    }

}
