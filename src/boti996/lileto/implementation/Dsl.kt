package boti996.lileto.implementation

import boti996.lileto.implementation.models.*
import boti996.lileto.implementation.parser.CommandParser
import boti996.lileto.implementation.parser.Operation
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

private val ASSIGN = CommandParser.OperatorTypes.ASSIGN
private val DECLARE = CommandParser.OperatorTypes.DECLARE
// TODO: registry-level checking even here
abstract class AbstractAssignBuilder(variableName: String, type: CommandParser.OperatorTypes) {
    init { assert(type in  listOf(ASSIGN, DECLARE)) }
    private val operations = Operation(op = type.value.joinToString(""),  left = variableName)
    private val lastValue = Operation()

    private operator fun Operation.plusAssign(operation: Operation) {
        var lastOperation = operation
        while (lastOperation.right != null) lastOperation = lastOperation.right!!

        lastOperation.right = operation
    }

    fun concatenate(variableName: String) {
        operations += Operation(op = CommandParser.OperatorTypes.CONCATENATE.value.joinToString(""), left = variableName)
    }
    fun concatenate(bracket: CommandContent) {
        operations += Operation(op = CommandParser.OperatorTypes.CONCATENATE.value.joinToString(""),
            // TODO: not the same as BracketWithContent hash, might cause problems
            left = "_${bracket.hashCode()}")
    }

    fun insert(variableName: String) {
        operations += Operation(op = CommandParser.OperatorTypes.INSERT.value.joinToString(""), left = variableName)
    }

    fun insert(bracket: CommandContent) {
        operations += Operation(op = CommandParser.OperatorTypes.INSERT.value.joinToString(""),
            // TODO: not the same as BracketWithContent hash, might cause problems
            left = "_${bracket.hashCode()}")
    }

    // TODO: this always be the last element NOTE: not neccessary if last value will be defined like Operation(op, left, null)
    fun lastValue(variableName: String) { lastValue.left = variableName }

    fun lastValue(bracket: CommandContent) { lastValue.left = "_${bracket.hashCode()}" }

    fun build(): Operation {
        assert(lastValue.left != null)
        operations += lastValue
        return operations
    }
}

@LiletoDsl
class AssignBuilder(variableName: String): AbstractAssignBuilder(variableName, ASSIGN)

@LiletoDsl
class DeclareBuilder(variableName: String): AbstractAssignBuilder(variableName, DECLARE)

@LiletoDsl
class command(variableName: String?) {
    // TODO: register all elements in local registry
    private val operations = mutableListOf<Operation>()
    private var output = null

    fun assign(variableName: String) {
        val assignBuilder = AssignBuilder(variableName)
        operations += assignBuilder.build()
    }

    fun declare(variableName: String) {
        val declareBuilder = DeclareBuilder(variableName)
        operations += declareBuilder.build()
    }

    fun output(variableName: String) {

    }
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

    // TODO: register all named elements in global registry

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
