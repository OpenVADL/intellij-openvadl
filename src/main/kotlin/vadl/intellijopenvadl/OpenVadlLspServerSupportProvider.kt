package vadl.intellijopenvadl

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspCommunicationChannel
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.DiagnosticCapabilities
import org.eclipse.lsp4j.DocumentHighlightCapabilities
import org.eclipse.lsp4j.MarkdownCapabilities
import org.eclipse.lsp4j.PublishDiagnosticsCapabilities
import java.io.File
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
            OpenVadlIcons.PluginIcon,
            OpenVadlSettingsConfigurable::class.java
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
        val openVadlPath = findOpenVadlExecutable()

        if (openVadlPath == null) {
            showNotAvailableNotification()
            throw IllegalStateException("OpenVADL LSP server not found. Please install OpenVADL or configure a custom path in settings.")
        }

        return GeneralCommandLine(openVadlPath, "lsp")
    }

    private fun findOpenVadlExecutable(): String? {
        val settings = OpenVadlSettings.getInstance()

        // First, try custom path if configured
        if (settings.customOpenVadlPath.isNotBlank()) {
            val customFile = File(settings.customOpenVadlPath)
            if (customFile.exists() && customFile.canExecute()) {
                return customFile.absolutePath
            }
        }

        // Fall back to PATH lookup
        return findInPath()
    }

    private fun findInPath(): String? {
        return try {
            val pathEnv = System.getenv("PATH") ?: return null
            val pathDirs = pathEnv.split(File.pathSeparator)
            val executableName = if (SystemInfo.isWindows) "openvadl.exe" else "openvadl"

            pathDirs.firstNotNullOfOrNull { dir ->
                val executable = File(dir, executableName)
                if (executable.exists() && executable.canExecute()) {
                    executable.absolutePath
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun showNotAvailableNotification() {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("OpenVADL")
            .createNotification(
                "OpenVADL Compiler Not Found",
                "The 'openvadl' command was not found in your PATH. Please install OpenVADL or configure a custom path in settings.",
                NotificationType.ERROR
            )

        notification.addAction(NotificationAction.createSimple("Open Settings") {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, OpenVadlSettingsConfigurable::class.java)
        })

        notification.notify(project)
    }


}
