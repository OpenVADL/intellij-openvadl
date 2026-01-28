package vadl.intellijopenvadl

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JPanel

class OpenVadlSettingsConfigurable : Configurable {
    private var customPathField: TextFieldWithBrowseButton? = null
    private val settings = OpenVadlSettings.getInstance()

    override fun getDisplayName(): String = "OpenVADL"

    override fun createComponent(): JComponent {
        customPathField = TextFieldWithBrowseButton().apply {
            val fileChooserDescriptor = FileChooserDescriptor(
                true,  // chooseFiles
                false, // chooseFolders
                false, // chooseJars
                false, // chooseJarsAsFiles
                false, // chooseJarContents
                false  // chooseMultiple
            )
                .withTitle("Select OpenVADL Executable")
                .withDescription("Choose the path to the openvadl executable")

            addBrowseFolderListener(null, fileChooserDescriptor)
            toolTipText = "Path to the openvadl executable. Leave empty to use the command from PATH."
        }

        val descriptionLabel = JBLabel(
            "<html>Specify a custom path to the OpenVADL executable.<br>" +
            "If left empty, the plugin will look for 'openvadl' in your system PATH.<br>" +
            "If you have the openvadl repo locally run `./gradlew jlink`<br>and select `vadl-cli/build/image/bin/openvadl`.</html>"
        ).apply {
            foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
            border = JBUI.Borders.emptyTop(5)
        }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent(
                JBLabel("Custom OpenVADL path:"),
                customPathField!!,
                1,
                false
            )
            .addComponentToRightColumn(descriptionLabel, 1)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        return customPathField?.text != settings.customOpenVadlPath
    }

    override fun apply() {
        val pathChanged = customPathField?.text != settings.customOpenVadlPath

        customPathField?.text?.let {
            settings.customOpenVadlPath = it
        }

        // Restart LSP servers for all open projects if the path changed
        if (pathChanged) {
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
    }

    override fun disposeUIResources() {
        customPathField = null
    }
}
