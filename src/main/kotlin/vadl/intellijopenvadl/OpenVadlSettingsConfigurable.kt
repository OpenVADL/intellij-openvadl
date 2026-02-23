package vadl.intellijopenvadl

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

class OpenVadlSettingsConfigurable : Configurable {
    private var customPathField: TextFieldWithBrowseButton? = null
    private var useTcpCheckBox: JBCheckBox? = null
    private var tcpPortField: JBTextField? = null
    private var dontStartServerCheckBox: JBCheckBox? = null
    private val settings = OpenVadlSettings.getInstance()

    override fun getDisplayName(): String = "OpenVADL"

    override fun createComponent(): JComponent {
        return panel {
            group("General Settings") {
                row("Custom OpenVADL path:") {
                    customPathField = textFieldWithBrowseButton(
                        FileChooserDescriptor(true, false, false, false, false, false)
                            .withTitle("Select OpenVADL Executable")
                            .withDescription("Choose the path to the openvadl executable"),
                        null
                    ).applyToComponent {
                        toolTipText = "Path to the openvadl executable. Leave empty to use the command from PATH."
                    }.align(AlignX.FILL).component
                }
                row {
                    comment(
                        "Specify a custom path to the OpenVADL executable.<br>" +
                                "If left empty, the plugin will look for 'openvadl' in your system PATH.<br>" +
                                "If you have the openvadl repo locally run <code>./gradlew jlink</code><br>and select <code>vadl-cli/build/image/bin/openvadl</code>."
                    )
                }
            }

            group("Connection Settings") {
                lateinit var useTcp: Cell<JBCheckBox>
                row {
                    useTcp = checkBox("Use TCP connection instead of stdin/stdout (not recommended)")
                    useTcpCheckBox = useTcp.component
                }
                indent {
                    row("TCP port:") {
                        tcpPortField = textField()
                            .applyToComponent {
                                toolTipText = "TCP port for LSP server connection"
                                columns = 8
                            }
                            .component
                    }
                    row {
                        dontStartServerCheckBox = checkBox("Connect to an already running server")
                            .comment("If checked, the plugin will not start the server and expects it to be already running on the specified port. This is for mostly for LSP development, if you are unsure leave it unchecked.")
                            .component
                    }
                }.enabledIf(useTcp.selected)
            }
        }
    }

    override fun isModified(): Boolean {
        return customPathField?.text != settings.customOpenVadlPath ||
                useTcpCheckBox?.isSelected != settings.useTcpConnection ||
                tcpPortField?.text?.toIntOrNull() != settings.tcpPort ||
                dontStartServerCheckBox?.isSelected != settings.dontStartServer
    }

    override fun apply() {
        val pathChanged = customPathField?.text != settings.customOpenVadlPath
        val tcpSettingsChanged = useTcpCheckBox?.isSelected != settings.useTcpConnection ||
                tcpPortField?.text?.toIntOrNull() != settings.tcpPort ||
                dontStartServerCheckBox?.isSelected != settings.dontStartServer

        customPathField?.text?.let {
            settings.customOpenVadlPath = it
        }
        useTcpCheckBox?.isSelected?.let {
            settings.useTcpConnection = it
        }
        tcpPortField?.text?.toIntOrNull()?.let {
            settings.tcpPort = it
        }
        dontStartServerCheckBox?.isSelected?.let {
            settings.dontStartServer = it
        }

        // Restart LSP servers for all open projects if settings changed
        if (pathChanged || tcpSettingsChanged) {
            restartLspServers()
        }
    }

    private fun restartLspServers() {
        ProjectManager.getInstance().openProjects.forEach { project ->
            val lspServerManager = LspServerManager.getInstance(project)
            lspServerManager.stopAndRestartIfNeeded(OpenVadlLspServerSupportProvider::class.java)
        }
    }

    override fun reset() {
        customPathField?.text = settings.customOpenVadlPath
        useTcpCheckBox?.isSelected = settings.useTcpConnection
        tcpPortField?.text = settings.tcpPort.toString()
        dontStartServerCheckBox?.isSelected = settings.dontStartServer
    }

    override fun disposeUIResources() {
        customPathField = null
        useTcpCheckBox = null
        tcpPortField = null
        dontStartServerCheckBox = null
    }
}
