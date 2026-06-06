package vadl.intellijopenvadl

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.PluginPathManager
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider
import kotlin.io.path.isDirectory

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
        val pluginRoot = PathManager.getPluginsDir().resolve("intellij-openvadl")
            .takeIf { it.isDirectory() }
            ?: PluginPathManager.getPluginHome("intellij-openvadl").toPath()
        val bundlePath = pluginRoot.resolve("textmate").resolve("openvadl").normalize()
        return listOf(TextMateBundleProvider.PluginBundle("OpenVADL", bundlePath))
    }
}
