package io.github.gmazzo.codeowners

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection

interface CodeOwnersSourceSet : Named {

     /**
      * The source files collection to be analyzed by the CodeOwners tasks.
      *
      * Usually you don't need to feed them manually, as they are automatically collected from the build
      */
     val sources : ConfigurableFileCollection

     /**
      * The compiled classes files collection to be analyzed by the CodeOwners tasks.
      *
      * Usually you don't need to feed them manually, as they are automatically collected from the build
      */
     val classes : ConfigurableFileCollection

     /**
      * The external mapping files (.codeowners) collection to be analyzed by the CodeOwners tasks.
      *
      * Usually you don't need to feed them manually, as they are automatically collected from the build
      */
     val mappings : ConfigurableFileCollection

}
