package references.builds.kotlinx.io

import BuildParams.KOTLINX_IO_ID
import BuildParams.KOTLINX_IO_TITLE
import jetbrains.buildServer.configs.kotlin.BuildType
import references.templates.PrepareDokkaTemplate

object KotlinxIOPrepareDokkaTemplates : BuildType({
    name = "$KOTLINX_IO_ID templates"
    description = "Build Dokka Templates for Kotlinx IO"

    templates(PrepareDokkaTemplate)

    params {
        param("env.ALGOLIA_INDEX_NAME", KOTLINX_IO_ID)
        param("env.API_REFERENCE_NAME", KOTLINX_IO_TITLE)
    }
})
