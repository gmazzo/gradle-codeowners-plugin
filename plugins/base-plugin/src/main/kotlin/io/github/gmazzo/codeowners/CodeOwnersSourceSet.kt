package io.github.gmazzo.codeowners

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection

interface CodeOwnersSourceSet : Named {

     val sources : ConfigurableFileCollection

     val classes : ConfigurableFileCollection

     val mappings : ConfigurableFileCollection

}
