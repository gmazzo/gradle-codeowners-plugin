plugins {
    id("io.github.gmazzo.codeowners.jvm")
}

codeOwners {
    rootDirectory = layout.projectDirectory
    codeOwnersRenamer { "jvm-$it" }
}
