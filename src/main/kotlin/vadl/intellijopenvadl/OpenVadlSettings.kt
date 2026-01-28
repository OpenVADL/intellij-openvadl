package vadl.intellijopenvadl

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "vadl.intellijopenvadl.OpenVadlSettings",
    storages = [Storage("openVadlSettings.xml")]
)
@Service
class OpenVadlSettings : PersistentStateComponent<OpenVadlSettings> {
    var customOpenVadlPath: String = ""

    override fun getState(): OpenVadlSettings = this

    override fun loadState(state: OpenVadlSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): OpenVadlSettings {
            return com.intellij.openapi.application.ApplicationManager.getApplication()
                .getService(OpenVadlSettings::class.java)
        }
    }
}
