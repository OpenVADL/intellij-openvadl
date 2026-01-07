package vadl.intellijopenvadl

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.markdown.utils.convertMarkdownToHtml
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspCommunicationChannel
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem
import com.intellij.platform.syntax.impl.builder.computeWithDiagnostics
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.DiagnosticCapabilities
import org.eclipse.lsp4j.DocumentHighlightCapabilities
import org.eclipse.lsp4j.DocumentHighlightOptions
import org.eclipse.lsp4j.MarkdownCapabilities
import org.eclipse.lsp4j.PublishDiagnosticsCapabilities
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.swing.Icon

object OpenVadlIcons {
    val PluginIcon: Icon = IconLoader.getIcon("/META-INF/pluginIcon.svg", OpenVadlIcons::class.java)
}

internal class OpenVadlLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
        if (file.extension == "vadl") {
            serverStarter.ensureServerStarted(OpenVadlLspServerDescriptor(project))
        }
    }

    override fun createLspServerWidgetItem(lspServer: LspServer, currentFile: VirtualFile?): LspServerWidgetItem? {
        return LspServerWidgetItem(
            lspServer, currentFile,
            OpenVadlIcons.PluginIcon, null
        )
    }

}

private class OpenVadlLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "OpenVADL") {
    override val lspCommunicationChannel: LspCommunicationChannel
        get() = LspCommunicationChannel.Socket(10999)

    override fun isSupportedFile(file: VirtualFile) = file.extension == "vadl"

    override val clientCapabilities: ClientCapabilities
        get() {
            return super.clientCapabilities.apply {
                textDocument.apply {
                    documentHighlight = DocumentHighlightCapabilities(true)
                    diagnostic = DiagnosticCapabilities(true)
                    publishDiagnostics = PublishDiagnosticsCapabilities(true)
                }
                general.apply {
                    markdown = MarkdownCapabilities()
                }
            }
        }

    override fun createCommandLine(): GeneralCommandLine {
        val lspRoot = extractLspServerToDisk()
        val serverBinary = File(lspRoot, "bin/openvadl-lsp")

        // Make sure the binary is executable
        serverBinary.setExecutable(true)

        // Start the server process (it will listen on TCP port 10999)
        return GeneralCommandLine(serverBinary.absolutePath).apply {
            withWorkDirectory(lspRoot)
        }
    }

    private fun extractLspServerToDisk(): File {
        // Create extraction directory in plugin system directory
        val pluginSystemDir = File(System.getProperty("user.home"), ".openvadl-lsp")

        // Extract LSP server files if not already extracted
        if (pluginSystemDir.exists()) {
            pluginSystemDir.deleteRecursively()
        }
        pluginSystemDir.mkdirs()

        // Extract bin/ and lib/ directories
        extractDirectory("openvadl-lsp/bin", File(pluginSystemDir, "bin"))
        extractDirectory("openvadl-lsp/lib", File(pluginSystemDir, "lib"))

        return pluginSystemDir
    }

    // Extracts the resource path describing a directory and copies it to the provided target directory on disk.
    private fun extractDirectory(resourcePath: String, targetDir: File) {
        targetDir.mkdirs()

        val classLoader = javaClass.classLoader
        val resourceUrl = classLoader.getResource(resourcePath) ?: return

        // Handle resources inside JAR files
        if (resourceUrl.protocol == "jar") {
            val jarPath = resourceUrl.path.substringAfter("file:").substringBefore("!")
            val decodedJarPath = java.net.URLDecoder.decode(jarPath, "UTF-8")
            val jarFile = java.util.jar.JarFile(decodedJarPath)

            jarFile.entries().asSequence()
                .filter { it.name.startsWith(resourcePath) && !it.isDirectory }
                .forEach { entry ->
                    val relativePath = entry.name.removePrefix("$resourcePath/")
                    if (relativePath.isNotEmpty()) {
                        val targetFile = File(targetDir, relativePath)
                        targetFile.parentFile.mkdirs()

                        classLoader.getResourceAsStream(entry.name)?.use { input ->
                            Files.copy(input, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                    }
                }
            jarFile.close()
        } else {
            // Handle resources from filesystem (development mode)
            val sourceDir = File(resourceUrl.toURI())
            sourceDir.walkTopDown()
                .filter { it.isFile }
                .forEach { sourceFile ->
                    val relativePath = sourceFile.relativeTo(sourceDir).path
                    val targetFile = File(targetDir, relativePath)
                    targetFile.parentFile.mkdirs()
                    Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
        }
    }


}
