package vadl.intellijopenvadl

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspCommunicationChannel
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspCustomization
import com.intellij.platform.lsp.api.customization.LspDiagnosticsSupport
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.DiagnosticCapabilities
import org.eclipse.lsp4j.DocumentHighlightCapabilities
import org.eclipse.lsp4j.MarkdownCapabilities
import org.eclipse.lsp4j.PublishDiagnosticsCapabilities
import java.io.File
import java.time.Duration
import java.time.Instant
import javax.swing.Icon

object OpenVadlIcons {
    val PluginIcon: Icon = IconLoader.getIcon("/META-INF/pluginIcon.svg", OpenVadlIcons::class.java)
}

internal class OpenVadlLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
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
        get() {
            val settings = OpenVadlSettings.getInstance()
            return if (settings.useTcpConnection) {
                LspCommunicationChannel.Socket(settings.tcpPort)
            } else {
                LspCommunicationChannel.StdIO
            }
        }

    override fun isSupportedFile(file: VirtualFile) = file.extension == "vadl"

    override val lspCustomization: LspCustomization = object : LspCustomization() {
        override val diagnosticsCustomizer = object : LspDiagnosticsSupport() {

            /**
             * Format the LSP diagnostic usable for the IDE.
             * At the moment the LSP reports diagnostics in plain text but the IDE expects HTML, so this is the function
             * that makes them work with each other.
             *
             * 1) It escapes all xml entries from the LSP diagnostic so that nothing get's wrongly interpreted as code.
             * 2) The first line is always a title so let's put it in bold.
             * 3) All linebreaks are replaced by break tags.
             */
            override fun getTooltip(diagnostic: Diagnostic): String {
                val message =  StringUtil.escapeXmlEntities(diagnostic.message)
                var lines = message.split("\n")
                lines = listOf("<b>${lines.first()}</b>") + lines.drop(1)
                return lines.joinToString("<br>")
            }
        }
    }

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
        val settings = OpenVadlSettings.getInstance()

        // If using TCP and don't start server, return a wild hack by spawning a useless process that runs forever
        // with which we won't interact.
        if (settings.useTcpConnection && settings.dontStartServer) {
            return if (SystemInfo.isWindows) {
                // Windows: Use pause command which waits for any key press
                GeneralCommandLine("timeout", "/t", "-1")
            } else {
                // Unix: cat waits for stdin indefinitely
                GeneralCommandLine("cat")
            }
        }

        val openVadlPath = findOpenVadlExecutable()

        if (openVadlPath == null) {
            showCompilerNotFoundNotification(settings.customOpenVadlPath)
            throw IllegalStateException("OpenVADL LSP server not found. Please install OpenVADL or configure a custom path in settings.")
        }

        // Check if the compiler is outdated (older than 2 weeks)
        checkCompilerAge(openVadlPath)

        val commandLine = GeneralCommandLine(openVadlPath, "lsp")

        // Disable syntax highlighting from LSP as we use TextMate grammar instead
        commandLine.addParameter("--no-syntax-highlighting")

        // Add TCP port argument if TCP mode is enabled
        if (settings.useTcpConnection) {
            commandLine.addParameter("--port")
            commandLine.addParameter(settings.tcpPort.toString())
        }

        return commandLine
    }

    override fun startServerProcess(): OSProcessHandler {
        val handler = super.startServerProcess().apply {
            addProcessListener(object : ProcessListener {
                override fun processTerminated(event: ProcessEvent) {
                    if (event.exitCode != 0) {
                        showErrorNotification(
                            "OpenVADL server terminated",
                            "The language server exited prematurely with code ${event.exitCode}."
                        )
                    }
                }
            })
        }
        return handler;
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


    private fun showCompilerNotFoundNotification(customPath: String = "") = showErrorNotification(
        "OpenVADL compiler not found",
        if (customPath.isNotBlank()) {
            "The 'openvadl' compiler was not found at '$customPath'. Double check the provided path in the settings."
        } else {
            "The 'openvadl' compiler was not found in your PATH. Please install OpenVADL or configure a custom path in settings."
        },
        true
    )

    private fun showErrorNotification(title: String, message: String, withSettings: Boolean = false) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("OpenVADL")
            .createNotification(title, message, NotificationType.ERROR)

        if (withSettings) {
            notification.addAction(NotificationAction.createSimple("Open Settings") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, OpenVadlSettingsConfigurable::class.java)
            })
        }

        notification.notify(project)
    }

    /**
     * Checks if the compiler binary is older than 2 weeks and shows a warning notification if so.
     * @param openVadlPath The path to the openvadl compiler binary
     */
    private fun checkCompilerAge(openVadlPath: String) {
        val compilerFile = File(openVadlPath)
        val lastModified = compilerFile.lastModified()


        if (lastModified <= 0) {
            // Could not determine modification date
            return
        }

        val lastModifiedInstant = Instant.ofEpochMilli(lastModified)
        val now = Instant.now()
        val ageInDays = Duration.between(lastModifiedInstant, now).toDays()
        
        // Threshold: 14 days (2 weeks)
        val thresholdDays = 14
        
        if (ageInDays > thresholdDays) {
            val notification = NotificationGroupManager.getInstance()
                .getNotificationGroup("OpenVADL")
                .createNotification(
                    "OpenVADL compiler is outdated",
                    "Your OpenVADL compiler was last updated $ageInDays days ago. Please consider updating it by rebuilding from the latest source.",
                    NotificationType.WARNING
                )
            
            notification.addAction(NotificationAction.createSimple("Open Settings") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, OpenVadlSettingsConfigurable::class.java)
            })
            
            notification.notify(project)
        }
    }

}
