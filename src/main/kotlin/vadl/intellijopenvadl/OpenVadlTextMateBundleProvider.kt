package vadl.intellijopenvadl

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider

/**
 * Registers the bundled OpenVADL TextMate grammar so that `.vadl` files get
 * syntax highlighting.
 *
 * The bundle is shipped as a plain directory at `<plugin>/textmate/openvadl`
 * (see the `prepareSandbox` copy step in build.gradle.kts) rather than inside
 * the plugin jar, because the TextMate reader needs a real filesystem path.
 */
class OpenVadlTextMateBundleProvider : TextMateBundleProvider {
    override fun getBundles(): List<TextMateBundleProvider.PluginBundle> {
        val plugin = PluginManager.getInstance()
            .findEnabledPlugin(PluginId.getId("vadl.intellij-openvadl"))
            ?: return emptyList()
        val bundlePath = plugin.pluginPath.resolve("textmate").resolve("openvadl")
        return listOf(TextMateBundleProvider.PluginBundle("OpenVADL", bundlePath))
    }
}
