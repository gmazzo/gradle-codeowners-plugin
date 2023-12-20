plugins {
    com.github.gmazzo.buildconfig
}

buildConfig {
    buildConfigField("String", "COMPILER_PLUGIN_ID", "\"io.github.gmazzo.codeowners\"")
    buildConfigField("String", "ARG_CODEOWNERS_ROOT", "\"codeownersRoot\"")
    buildConfigField("String", "ARG_CODEOWNERS_FILE", "\"codeownersFile\"")
}
