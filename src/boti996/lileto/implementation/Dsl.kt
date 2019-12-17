package boti996.lileto.implementation

import boti996.lileto.implementation.models.Bracket
import boti996.lileto.implementation.models.LiletoContext
import boti996.lileto.implementation.models.TextContext
import boti996.lileto.implementation.parser.SpecialCharacter

@DslMarker
annotation class LiletoDsl

/**
 * Special character bracket
 * @constructor initialize with one or multiple special characters
 */
@LiletoDsl
data class SpecialCharBuilder(val chars: List<SpecialCharacter>) {
    /** @param char special character */
    constructor(char: SpecialCharacter): this(listOf(char))

    fun build() = TextContext(chars.joinToString("") {
            specialCharacter -> specialCharacter.character()
    })
}

/**
 * Text bracket
 * */
@LiletoDsl
data class TextBuilder(val text: String = "") {
    private val texts = mutableListOf<TextContext>()

    operator fun String.unaryPlus() {
        texts += TextContext(this)
    }

    fun spec(chars: List<SpecialCharacter>, setup: SpecialCharBuilder.() -> Unit) {
        val specialCharBuilder = SpecialCharBuilder(chars)
        specialCharBuilder.setup()
        texts += specialCharBuilder.build()
    }

    fun spec(char: SpecialCharacter, setup: SpecialCharBuilder.() -> Unit) {
        val specialCharBuilder = SpecialCharBuilder(char)
        specialCharBuilder.setup()
        texts += specialCharBuilder.build()
    }

    fun build() = TextContext("$text${texts.joinToString("") { it.evaluate() }}")
}


@LiletoDsl
class command {
    // TODO
}


@LiletoDsl
class container {
    // TODO
}


@LiletoDsl
class template {
    // TODO
}

/**
 * It contains all the other brackets
 */
@LiletoDsl
class LiletoContextBuilder {
    private val children = mutableListOf<Bracket>()

    operator fun Bracket.unaryPlus() {
        children += this
    }

    //TODO

    fun build() = LiletoContext.evaluate(children)

    @Suppress("UNUSED_PARAMETER")
    @Deprecated(level = DeprecationLevel.ERROR, message = "LiletoContexts can't be nested.")
    fun liletoContext(param: () -> Unit = {}) {}
}

/**
 * Root element
 * @return the evaluated Lileto code
 */
@LiletoDsl
fun liletoContext(setup: LiletoContextBuilder.() -> Unit): String {
    val liletoContextBuilder = LiletoContextBuilder()
    liletoContextBuilder.setup()
    return liletoContextBuilder.build()
}

/**
 * Comments
 * Does absolutely nothing
 */
@LiletoDsl
fun comment(message: String) {}
