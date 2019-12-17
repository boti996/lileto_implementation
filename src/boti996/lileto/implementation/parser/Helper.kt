package boti996.lileto.implementation.parser

import boti996.lileto.implementation.models.mustBeImplemented

/**
 * Store the literals of Lileto brackets
 * @param literals opening and closing characters
 */
enum class BracketType(private val literals: Pair<Char, Char>) {
    TEXT(Pair('"', '"')),
    TEMPLATE(Pair('<', '>')),
    SPECIAL_CHAR(Pair('$', '$')) {
        override fun isEndingMarkSkippable() = true
    },
    COMMAND(Pair('!', '!')) {
        override fun isEndingMarkSkippable() = true
    },
    CONTAINER(Pair('[', ']')),
    COMMENT(Pair('.', '.')),
    LILETO_CONTEXT(Pair('Ł', 'Ł')) {    // TODO: rethink this solution - root element
        override fun open() = ""
        override fun close() = ""
    };

    open fun open() = "{${literals.first}"
    open fun close() = "${literals.second}}"
    open fun isEndingMarkSkippable() = false
}

/**
 * A 2 Char long shift-register ued for recognizing parse brackets
 */
data class BracketBuffer(private var buffer: String = "", private var content: String = "") {
    fun push(char: Char) {
        assert(buffer.length <=2)

        content += char

        if (buffer.length == 2) {

            buffer = "${buffer[1]}$char"

        } else if (buffer.length == 1) {

            buffer = "${buffer[0]}$char"

        } else {    // empty

            buffer = "$char"
        }
    }

    fun get() = buffer
    fun set(value: String) { buffer = value }

    fun getContent() = content
    fun clearContent() { content = "" }
    fun corrigateContent_endingMarkSkipped() { content += "x" }
}

enum class WhichBracket {
    OPENING, CLOSING
}

internal val opening = WhichBracket.OPENING
internal val closing = WhichBracket.CLOSING

fun getBracketType(bracketBuffer: BracketBuffer, which: WhichBracket, peekBracket: BracketWithContent) : BracketType? {

    val bracket = bracketBuffer.get()
    BracketType.values().forEach { bracketType ->
        if (which == opening) {
            if (bracketType.open() == bracket) {
                return bracketType
            }
        } else {
            if (bracketType.close() == bracket) {
                return bracketType
            }
        }
    }

    if (peekBracket.bracket.isEndingMarkSkippable() &&
            which == closing &&
            bracket[1] == '}') {
        bracketBuffer.corrigateContent_endingMarkSkipped()
        return peekBracket.bracket
    }

    return null
}


/**
 * Store bracket with it's content
 * @param bracket type of bracket
 */
data class BracketWithContent(val bracket: BracketType, var content: MutableList<Any> = mutableListOf()) {

    val userErrorInfo = UserErrorInfo()

    fun open() = bracket.open()
    fun close() = bracket.close()

    var isClosed: Boolean = false
    override fun toString() =
        when (bracket) {
            BracketType.SPECIAL_CHAR -> evaluateSpecialChars(content.joinToString(""), userErrorInfo)
            BracketType.TEXT -> content.joinToString("")
            else -> open() +
                    content.joinToString("") +
                    if (isClosed) close() else ""
    }

    fun getTemporaryRegistryName() = "_${this.hashCode()}"

    companion object {
        // values used for special char bracket evaluation
        private val sortedForRegex = SpecialCharacter.values()
            .map { specialCharacter ->  specialCharacter.literal() }
            .sortedByDescending { literal -> literal.length }

        private const val commaAndWhitespaceSeparation = "\\s*,?\\s*"

        private val patternValidation = Regex("(${sortedForRegex.joinToString("|")}|$commaAndWhitespaceSeparation)+")

        private val patternFindSpecialChars = Regex("(${sortedForRegex.joinToString("|")})?")

        private fun getParsedSpecialChars(content: String)
                = patternFindSpecialChars.findAll(content)
            .map { result -> result.groupValues[1] }
            .filter { result -> result.isNotBlank() }
            .toList()
            .map { literal -> SpecialCharacter.get(literal) }

        private fun evaluateSpecialChars(textContent: String, userErrorInfo: UserErrorInfo): String {
            if (!patternValidation.matches(textContent)) {
                throw IllegalArgumentException("There was a problem during parsing " +
                        "in special character bracket with the following content:\n" +
                        "line:${userErrorInfo.line}:${userErrorInfo.column}\t-->\t\t$textContent")
            }
            return getParsedSpecialChars(textContent)
                .joinToString("") {
                        specialChar-> specialChar?.character() ?: mustBeImplemented("Implementation error has occurred")
                }
        }
    }


}

/**
 * Store the literals of Lileto special characters
 * @param values Lileto literal and the resolved values
 */
enum class SpecialCharacter(private val values: Pair<String, Char>) {
    ENTER       (Pair("e", '\n')),
    TAB         (Pair("t", '\t')),
    SPACE       (Pair("s", ' ')),
    O_R_BRACKET (Pair("orb", '(')),
    C_R_BBRACKET(Pair("crb", ')')),
    EQUALS_SIGN (Pair("eq", '=')),
    PLUS_SIGN   (Pair("pl", '+')),
    O_C_BRACKET (Pair("ocb", '{')),
    C_C_BRACKET (Pair("ccb", '}')),
    POINT       (Pair("pe", '.')),
    COMMA       (Pair("co", ',')),
    SEMICOLON   (Pair("sc", ';')),
    COLON       (Pair("cl", ':')),
    VBAR        (Pair("pi", '|')),
    LT_SIGN     (Pair("ls", '<')),
    GT_SIGN     (Pair("gt", '>')),
    QUESTION_M  (Pair("qm", '?')),
    EXCLAM_M    (Pair("dm", '!'));

    fun literal() = values.first
    fun character() = values.second.toString()

    companion object {
        fun get(literal: String) = values().find { specialCharacter -> specialCharacter.literal() == literal }
    }
}
