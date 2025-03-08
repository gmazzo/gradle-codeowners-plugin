plugins {
    com.github.gmazzo.buildconfig
}

buildConfig {
    buildConfigField("COMPILER_PLUGIN_ID", "io.github.gmazzo.codeowners")
    buildConfigField("ARG_CODEOWNERS_ROOT", "codeownersRoot")
    buildConfigField("ARG_CODEOWNERS_FILE", "codeownersFile")
    buildConfigField("ARG_MAPPINGS_OUTPUT", "mappingsOutput")
}
