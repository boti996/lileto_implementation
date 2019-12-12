package boti996.lileto.implementation.parser

import boti996.lileto.implementation.models.LiletoContext
import java.util.*


fun trimContent(content: String, peekBracket: BracketWithContent): String {
    return when(peekBracket.bracket) {
        BracketType.LILETO_CONTEXT -> content.substring(0 until content.length-2)
        else -> {
           content.substring(0 until content.length-2)
                .let { if (peekBracket.content.size == 0) it.trimStart() else it }     // first element
                .let { if (peekBracket.isClosed) it.trimEnd() else it }                 // last element
        }
    }
}

data class UserErrorInfo(val line: Int = UserErrorInfo.line,
                         val column: Int = UserErrorInfo.column,
                         val charNumber: Int = UserErrorInfo.charNumber) {
    companion object {
        var line: Int = 1; private set
        fun increaseLine() { line++; column = 1 }

        var column: Int = 1; private set
        var charNumber: Int = 1; private set
        fun increaseCharNumber() { charNumber++; column++ }
    }
}

fun plaintextToLiletoParser(plaintext: String) = plaintextToLiletoContext(plaintext).evaluate()

private fun plaintextToLiletoContext(plaintext: String) : LiletoContext {

    val bracketStack = Stack<BracketWithContent>() // this contains all of the open brackets

    val bracketBuffer = BracketBuffer()

    fun throwParsingError() : Nothing {
        val trimmed = plaintext
            .take(UserErrorInfo.charNumber)
            .takeLastWhile { char -> char != '\n' }
        throw IllegalArgumentException("There was a problem during parsing at\n" +
                "line:${UserErrorInfo.line}:${UserErrorInfo.column}\t-->\t\t${trimmed.takeLast(80)}")
    }

    bracketStack.push(BracketWithContent(BracketType.LILETO_CONTEXT))   // root element
    //TODO: bug - in comment and text brackets it shouldn't trim inner brackets
    for (char in plaintext) {
        bracketBuffer.push(char)

        val peekBracket = bracketStack.peek()
        // '{M' comes
        getBracketType(bracketBuffer, opening, peekBracket)?.let { bracketType ->

            val actualBracket = BracketWithContent(bracketType)
            val stringContent = trimContent(bracketBuffer.getContent(), peekBracket)
            bracketBuffer.clearContent()

            peekBracket.content.add(stringContent)
            peekBracket.content.add(actualBracket)

            bracketStack.push(actualBracket)
        }
        // 'M}' or '}' comes
        getBracketType(bracketBuffer, closing, peekBracket)?.let { bracketType ->

            val actualBracket: BracketWithContent =
                when (bracketType) {
                    peekBracket.bracket -> peekBracket
                    else -> throwParsingError()
                }
            peekBracket.isClosed = true
            val stringContent = trimContent(bracketBuffer.getContent(), peekBracket)
            bracketBuffer.clearContent()

            peekBracket.content.add(stringContent)
            bracketStack.pop()
        }
        UserErrorInfo.increaseCharNumber()
        if (char == '\n') UserErrorInfo.increaseLine()
    }

    val rootContext = bracketStack.pop()
    rootContext.content.add(bracketBuffer.getContent())
    rootContext.isClosed = true

    return LiletoContext(rootContext)
}
