package vadl.intellijopenvadl

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object VadlLanguage : Language("VADL", "text/vadl") {
    override fun getDisplayName(): String = "VADL"
    private fun readResolve(): Any = VadlLanguage
}

class VadlFileType : LanguageFileType(VadlLanguage) {
    override fun getName() = "VADL"
    override fun getDescription() = "The Vienna Architecture Description Language"
    override fun getDefaultExtension() = "vadl"
    override fun getIcon(): Icon = OpenVadlIcons.PluginIcon
}
