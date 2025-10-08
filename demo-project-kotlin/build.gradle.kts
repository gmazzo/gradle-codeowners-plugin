plugins {
    id("io.github.gmazzo.codeowners.kotlin")
}

codeOwners {
    rootDirectory = layout.projectDirectory
    codeOwnersRenamer { "kt-$it" }

    reports {
        ignoreUnowned()
        checkstyle.required = true
        sarif.required = true
    }
}
