package vadl.intellijopenvadl

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiLiteralValue

class VadlLanguageInjector : MultiHostInjector {

    private val keywords = setOf("instruction set architecture")

    override fun elementsToInjectIn(): List<Class<out PsiElement>> =
        listOf(PsiLiteralValue::class.java)

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val lit = context as? PsiLiteralValue ?: return
        val host = lit as? PsiLanguageInjectionHost ?: return
        if (!host.isValidHost) return

        val value = lit.value as? String ?: return
        if (keywords.none { value.contains(it) }) return

        val text = lit.text ?: return

        val rangeInHost = when {
            // normal Java string: "..."
            text.length >= 2 && text.first() == '"' && text.last() == '"' ->
                TextRange(1, text.length - 1)

            // Java text block: """ ... """
            text.startsWith("\"\"\"") && text.endsWith("\"\"\"") && text.length >= 6 ->
                TextRange(3, text.length - 3)

            else -> return
        }

        registrar.startInjecting(VadlLanguage)
        registrar.addPlace(null, null, host, rangeInHost)
        registrar.doneInjecting()
    }
}
