plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = "vadl"
version = "0.0.6"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}


dependencies {
    intellijPlatform {
        intellijIdea("2025.2.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here:
    }
}



intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.1"
        }

        changeNotes = """
           <h1 id="section">0.0.6</h1>
            <h2 id="new-features">New Features</h2>
            <ul>
            <li>Improve diagnostic formatting (no longer a single line in the popup)</li>
            </ul>
            <h2 id="requirements">Requirements</h2>
            <ul>
            <li>This plugin works only on IntelliJ &gt;= 2025.2.1</li>
            <li>Local installation of the openvadl compiler</li>
            </ul>
            <h1 id="005">0.0.5</h1>
            <h2 id="new-features">New Features</h2>
            <ul>
            <li>Dialog with information when the server crashes</li>
            <li>Automatic restarting the server if the settings change</li>
            <li>Change recommended custom build to JLink</li>
            </ul>
            <h2 id="requirements">Requirements</h2>
            <ul>
            <li>This plugin works only on IntelliJ &gt;= 2025.2.1</li>
            <li>Local installation of the openvadl compiler</li>
            </ul>
            <h1 id="004">0.0.4</h1>
            <h2 id="new-features">New Features</h2>
            <ul>
            <li>No OS nor architecture dependency</li>
            <li>Drop IntelliJ required version from 2025.3 to 2025.2.1</li>
            </ul>
            <p>This release switches the language server strategy from bundling the
            LS with the plugin to requiring that the compiler is somewhere on the
            developer machine. By default the plugin will try to load it from the
            PATH but users can specify custom locations.</p>
            <p>The whole discussion can be found <a
            href="https://github.com/OpenVADL/openvadl/issues/674">here</a>.</p>
            <h2 id="requirements">Requirements</h2>
            <ul>
            <li>This plugin works only on IntelliJ &gt;= 2025.2.1</li>
            <li>Local installation of the openvadl compiler</li>
            </ul>
            <h1 id="003">0.0.3</h1>
            <h2 id="warning-warning">WARNING ⚠️</h2>
            <p>This release only works on macOS-arm. This wasn't intentional and a
            mistake during the packaging setup. Fixed in the next release</p>
            <h2 id="new-features-1">New Features</h2>
            <ul>
            <li>No longer is it required that the user has a Java25
            installation</li>
            <li>Updated underlying LSP (more and better diagnostics)</li>
            </ul>
            <h2 id="requirements-1">Requirements</h2>
            <ul>
            <li>This plugin works only on IntelliJ &gt;= 2025.3</li>
            <li>macOS with arm chip</li>
            </ul>
            <h1 id="002">0.0.2</h1>
            <h2 id="new-features-2">New Features</h2>
            <ul>
            <li>Windows support</li>
            <li>Icons</li>
            </ul>
            <h2 id="requirements-2">Requirements</h2>
            <ul>
            <li>This plugin works only on IntelliJ &gt;= 2025.3</li>
            <li>This version requires you to have Java25 in your ${'$'}JAVA_HOME</li>
            </ul>
            <p>The plugin comes bundled with the LSP so no other setup, just be
            aware if you work on the LSP you <strong>need to disable this
            plugin</strong>. Because both will try to acquire port 10999.</p>
            <h1 id="001">0.0.1</h1>
            <p>This is the initial version of the language support plugin for
            IntelliJ.</p>
            <p>You can download the zip below and then go to Settings -&gt; Plugins
            -&gt; Install Plugin from Disk.</p>
            <p>The plugin comes bundled with the LSP so no other setup, just be
            aware if you work on the LSP you <strong>need to disable this
            plugin</strong>. Because both will try to acquire port 10999.</p>
            <p><strong>WARNING:</strong> This version has no Windows support
            <strong>WARNING:</strong> This version requires you to have Java25 in
            your ${'$'}JAVA_HOME <strong>WARNING:</strong> This version only works in
            IntelliJ 2025.3</p>
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
