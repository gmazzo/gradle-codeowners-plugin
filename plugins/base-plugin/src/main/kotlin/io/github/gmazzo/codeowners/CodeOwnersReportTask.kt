package io.github.gmazzo.codeowners

import groovy.json.JsonBuilder
import io.github.gmazzo.codeowners.matcher.CodeOwnersFile
import io.github.gmazzo.codeowners.matcher.CodeOwnersMatcher
import java.io.File
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import org.apache.bcel.util.ClassPath
import org.apache.bcel.util.ClassPathRepository
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Document

@CacheableTask
public abstract class CodeOwnersReportTask : DefaultTask() {

    @get:Internal
    public abstract val rootDirectory: DirectoryProperty

    @get:Input
    @Suppress("unused")
    protected val rootDirectoryPath: Provider<String> = project.rootDir.let { rootDir ->
        rootDirectory.map { it.asFile.toRelativeString(rootDir) }
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val codeOwnersFile: RegularFileProperty

    @get:Internal
    public abstract val sources: ConfigurableFileCollection

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    protected val sourcesFiles: FileTree = sources.asFileTree

    @get:Internal
    public abstract val classes: ConfigurableFileCollection

    @get:Internal
    public abstract val mappings: ConfigurableFileCollection

    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @Suppress("unused")
    protected val classesAndMappingFiles: FileTree = classes.asFileTree + mappings.asFileTree

    @get:Nested
    public abstract val reports: CodeOwnersReports

    private val pathsBaseDir = project.projectDir

    init {
        outputs.doNotCacheIf("SARIF reports requires absolute paths") {
            (it as CodeOwnersReportTask).reports.sarif.required.get()
        }
    }

    @TaskAction
    public fun reportCodeOwners() {
        val ownersWithFiles = mutableMapOf<String, SortedSet<File>>()
        val unownedFiles = sortedSetOf<File>()
        val byPathOwners = mutableMapOf<String, MutableSet<String>>()
        val severity = reports.unownedClassSeverity.get()
        val totalFiles =
            resolveCodeOwnersOfSourceFiles(ownersWithFiles, unownedFiles, byPathOwners, severity)
        val unownedPercent = unownedFiles.size * 100f / totalFiles
        val failedMessage =
            if (unownedPercent > reports.failOnUnownedThreshold.getOrElse(Float.POSITIVE_INFINITY))
                "Found ${unownedFiles.size} unowned class files out of $totalFiles (${"%.2f".format(unownedPercent)}%)"
            else null

        val xmlReport = lazy { generateXMLReport(totalFiles, ownersWithFiles, unownedFiles) }
        if (reports.mappings.required.get()) generateMappingReport(byPathOwners)
        if (reports.html.required.get()) generateHTMLReport(xmlReport, failedMessage)
        if (reports.xml.required.get()) xmlReport.value
        if (reports.checkstyle.required.get()) generateCheckstyleReport(unownedFiles, severity)
        if (reports.sarif.required.get()) generateSarifReport(unownedFiles, severity)

        check(failedMessage == null) { failedMessage!! }
    }

    private fun resolveCodeOwnersOfSourceFiles(
        ownersWithFiles: MutableMap<String, SortedSet<File>>,
        unownedFiles: MutableSet<File>,
        byPathOwners: MutableMap<String, MutableSet<String>>,
        severity: CodeOwnersReports.Severity,
    ): Int {
        val root = rootDirectory.asFile.get()
        val matcher = CodeOwnersMatcher(root, codeOwnersFile.asFile.get().useLines { CodeOwnersFile(it) })
        val logLevel = when (severity) {
            CodeOwnersReports.Severity.INFO -> LogLevel.INFO
            CodeOwnersReports.Severity.WARNING -> LogLevel.WARN
            CodeOwnersReports.Severity.ERROR -> LogLevel.ERROR
        }
        var count = 0

        sourcesFiles.visit {
            if (!isDirectory) {
                count++
                val owners = matcher.ownerOf(file, isDirectory = false)

                if (owners != null) {
                    byPathOwners.computeIfAbsent(path) { mutableSetOf() }.addAll(owners)

                    for (owner in owners) {
                        ownersWithFiles.computeIfAbsent(owner) { sortedSetOf() }.add(file)
                    }

                } else {
                    logger.log(logLevel, "Unowned class file: {}", file.path)
                    unownedFiles.add(file)
                }
                logger.info("File '{}' owners: {}", path, owners)
            }
        }
        return count
    }

    private fun expandCodeOwnersFromFilesToClasses(
        byPathOwners: Map<String, Set<String>>,
    ): MutableMap<String, MutableSet<String>> {
        val classEntries = TreeMap<String, MutableSet<String>>()
        val repository = ClassPathRepository(ClassPath(classes.asPath))

        classes.asFileTree.matching { include("**/*.class") }.visit {
            if (!isDirectory) {
                val className = path.removeSuffix(".class").replace('/', '.')
                val javaClass = repository.loadClass(className)
                val owners = byPathOwners[javaClass.sourceFilePath] ?: return@visit
                val entryPath = javaClass.className.replace('.', File.separatorChar)

                classEntries.computeIfAbsent(entryPath) { mutableSetOf() }.addAll(owners)
                logger.info("Class '{}' (of file '{}') owners: {}", className, javaClass.sourceFilePath, owners)
            }
        }
        return classEntries
    }

    private fun collectFromExternalMappings(
        entries: MutableMap<String, MutableSet<String>>
    ) = mappings.asFileTree.forEach { file ->
        file.useLines { CodeOwnersFile(it) }
            .entries
            .filterIsInstance<CodeOwnersFile.Entry>()
            .forEach {
                entries.computeIfAbsent(it.pattern) { mutableSetOf() }.addAll(it.owners)

                logger.info("Class '{}' (from mapping file '{}') owners: {}", it.pattern, file.path, it.owners)
            }
    }

    private fun generateMappingReport(byPathOwners: MutableMap<String, MutableSet<String>>) {
        val file = reports.mappings.outputLocation.asFile.get()

        val classEntries = expandCodeOwnersFromFilesToClasses(byPathOwners)
        collectFromExternalMappings(classEntries)

        if (classEntries.isEmpty()) {
            file.delete()
            return
        }

        val header = listOfNotNull(reports.mappings.header.orNull?.let(CodeOwnersFile::Comment))
        val codeOwners = CodeOwnersFile(header + classEntries.map { (key, value) ->
            CodeOwnersFile.Entry(pattern = key, owners = value.toList())
        })

        file.writeText(codeOwners.content)
    }

    private fun generateXMLReport(
        totalFiles: Int,
        ownersWithFiles: MutableMap<String, SortedSet<File>>,
        unownedFiles: SortedSet<File>,
    ): Document {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val root = doc.appendChild(doc.createElement("codeowners").apply {
            setAttribute("files", totalFiles.toString())
            setAttribute("unowned", unownedFiles.size.toString())
        })

        for ((owner, files) in ownersWithFiles) {
            root.appendChild(doc.createElement("owner").apply {
                setAttribute("name", owner)
                setAttribute("files", files.size.toString())

                for (file in files) {
                    appendChild(doc.createElement("file").apply {
                        setAttribute("path", file.toRelativeString(pathsBaseDir))
                    })
                }
            })
        }

        root.appendChild(doc.createElement("unowned").apply {
            setAttribute("files", unownedFiles.size.toString())

            for (file in unownedFiles) {
                appendChild(doc.createElement("file").apply {
                    setAttribute("path", file.toRelativeString(pathsBaseDir))
                })
            }
        })

        if (reports.xml.required.get()) {
            doc.writeTo(reports.xml.outputLocation)
        }
        return doc
    }

    private fun generateHTMLReport(xmlReport: Lazy<Document>, failedMessage: String?) {
        if (!reports.html.required.get()) return

        val stylesheet: String = reports.html.stylesheet.orNull
            ?: javaClass.classLoader.getResource("codeowners-check-report.xsl").readText()

        val transformer = TransformerFactory.newInstance()
            .newTransformer(StreamSource(stylesheet.byteInputStream()))
            .apply {
                setParameter("pluginURL", BuildConfig.PLUGIN_URL)
                setParameter("pluginVersion", BuildConfig.PLUGIN_VERSION)
                setParameter(
                    "relativePath",
                    pathsBaseDir.toRelativeString(reports.html.outputLocation.asFile.get().parentFile)
                )
                setParameter("failedMessage", failedMessage.orEmpty())
            }

        xmlReport.value.writeTo(reports.html.outputLocation, transformer)
    }

    private fun generateCheckstyleReport(
        unownedFiles: Set<File>,
        severity: CodeOwnersReports.Severity,
    ) {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val root = doc.appendChild(doc.createElement("checkstyle").apply {
            setAttribute("version", "9.3")
        })
        for (file in unownedFiles) {
            root.appendChild(doc.createElement("file").apply {
                setAttribute("name", file.toRelativeString(pathsBaseDir))

                appendChild(doc.createElement("error").apply {
                    setAttribute("severity", severity.name.lowercase())
                    setAttribute("message", "Class file does not have a code owner")
                    setAttribute("source", CodeOwnersReportTask::class.java.name)
                })
            })
        }

        doc.writeTo(reports.checkstyle.outputLocation)
    }

    private fun generateSarifReport(
        unownedFiles: Set<File>,
        severity: CodeOwnersReports.Severity,
    ) {
        val json = mapOf(
            $$"$schema" to "https://json.schemastore.org/sarif-2.1.0-rtm.4",
            "version" to "2.1.0",
            "runs" to listOf(
                mapOf(
                    "tool" to mapOf(
                        "driver" to mapOf(
                            "name" to CodeOwnersReportTask::class.java.name,
                            "informationUri" to BuildConfig.PLUGIN_URL,
                            "version" to BuildConfig.PLUGIN_VERSION,
                            "rules" to listOf(
                                mapOf(
                                    "id" to "UnownedClassFile",
                                    "shortDescription" to mapOf(
                                        "text" to "Class file does not have a code owner"
                                    ),
                                )
                            )
                        )
                    ),
                    "artifacts" to listOf(
                        "location" to mapOf(
                            "uri" to pathsBaseDir.toURI().toString()
                        )
                    ),
                    "results" to listOf(
                        mapOf(
                            "ruleId" to "UnownedClassFile",
                            "level" to severity.name.lowercase(),
                            "message" to mapOf(
                                "text" to "Class file does not have a code owner",
                            ),
                            "locations" to unownedFiles.map {
                                mapOf(
                                    "physicalLocation" to mapOf(
                                        "artifactLocation" to mapOf(
                                            "uri" to it.toURI().toString()
                                        )
                                    )
                                )
                            }
                        )
                    )
                )
            ))

        reports.sarif.outputLocation.get().asFile.apply {
            parentFile?.mkdirs()
            writeText(JsonBuilder(json).toPrettyString())
        }
    }

    private fun Document.writeTo(
        file: Provider<RegularFile>,
        transformer: Transformer = TransformerFactory.newInstance().newTransformer()
    ) = file.get().asFile.apply {
        parentFile?.mkdirs()
        outputStream().use { out ->
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.transform(DOMSource(this@writeTo), StreamResult(out))
        }
    }

}
