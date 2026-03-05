package vadl.intellijopenvadl.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.lexer.LexerBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import vadl.intellijopenvadl.VadlFileType
import vadl.intellijopenvadl.VadlLanguage

object VadlTokenTypes {
    val FILE = IFileElementType(VadlLanguage)
    val CONTENT = IElementType("CONTENT", VadlLanguage)
}

class VadlFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, VadlLanguage) {
    override fun getFileType(): FileType = VadlFileType()
    override fun toString() = "VADL File"
}

class VadlLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var start = 0
    private var end = 0
    private var tokenReturned = false

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.start = startOffset
        this.end = endOffset
        this.tokenReturned = false
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? =
        if (tokenReturned || start >= end) null else VadlTokenTypes.CONTENT

    override fun getTokenStart(): Int = start
    override fun getTokenEnd(): Int = end

    override fun advance() {
        tokenReturned = true
    }

    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = end
}


class VadlParser : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val mark = builder.mark()

        while (!builder.eof()) {
            builder.advanceLexer()
        }

        mark.done(root)
        return builder.treeBuilt
    }
}

class VadlPsiElement(node: ASTNode) : ASTWrapperPsiElement(node)

class VadlParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer = VadlLexer()
    override fun createParser(project: Project?): PsiParser = VadlParser()

    override fun getFileNodeType(): IFileElementType = VadlTokenTypes.FILE

    override fun getWhitespaceTokens(): TokenSet = TokenSet.EMPTY
    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

    override fun createElement(node: ASTNode): PsiElement = VadlPsiElement(node)
    override fun createFile(viewProvider: FileViewProvider) = VadlFile(viewProvider)
}
