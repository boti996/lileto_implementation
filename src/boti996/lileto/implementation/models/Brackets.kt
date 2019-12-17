package boti996.lileto.implementation.models

import boti996.lileto.implementation.parser.BracketType
import boti996.lileto.implementation.parser.BracketWithContent


fun notSupported() : Nothing = throw IllegalStateException("This operation is not supported")
fun mustBeImplemented(message: String) : Nothing = throw NotImplementedError(message)   // TODO: debug only


/**
 * Registry for accessing local Lileto variables
 */
class Registry {
    private val registry = mutableMapOf<String, Bracket>()
    /** @param variableName Lileto variable name
     * @return Lileto variable's value */
    operator fun get(variableName: String) = registry[variableName] ?: mustBeImplemented("Implementation error has occurred")
    /** @param variableName Lileto variable name
     * @param bracket Lileto variable's value
     * Register Lileto variable */
    operator fun set(variableName: String, bracket: Bracket) { registry[variableName] = bracket }
    /** @param variableName Lileto variable name
     * Unregister Lileto variable */
    fun remove(variableName: String) { registry.remove(variableName) }
    /** Used for accessing higher scope registries
     * @return immutable registry  */
    fun getRegistry(): Map<String, Bracket> = registry
}

/**
 * Base class for evaluating the parsed elements of Lileto code
 */
abstract class Bracket {
    /** Local Lileto registry */
    protected val localRegistry = Registry()

    /** @return evaluated Lileto code */
    abstract fun evaluate(): String
    /** @param other
     * @return deep copy of concatenated content in a new object */
    open fun concatenate(other: Bracket): Bracket = notSupported()
    /** @param other
     * Initialize current object with [other]'s content */
    open fun assign(other: Bracket): Unit = notSupported()

    /** @param other
     * @return deep copy of current object's content modified with [other]'s content in a new object */
    open fun insert(other: Bracket): Bracket = notSupported()

    /** @return deep copy of current object's content in a new object  */
    abstract fun deepCopy(): Bracket

    companion object {
        /** Global Lileto registry */
        val globalRegistry = Registry()

        /** @param bracket
         * @return deep copy of [bracket]'s content in a new object */
        fun declaration(bracket: Bracket): Bracket = bracket.deepCopy()
    }
}

/**
 * This parses and evaluate the Lileto code
 * @param rootContext root bracket of pre-parsed Lileto text input
 * @constructor Parse Lileto text input
 */
class LiletoContext(rootContext: BracketWithContent): Bracket() {

    private val brackets: List<Bracket>

    init {
        brackets = mutableListOf()
        rootContext.content.forEach {contentToParse ->

            when (contentToParse) {
                is String -> TextContext(contentToParse)
                is BracketWithContent -> parse(contentToParse)
                else -> notSupported()
            }
                ?.let { bracket -> brackets.add(bracket) }
        }
    }

    /** Copy constructor */
    constructor(brackets: List<Bracket>): this(BracketWithContent(BracketType.LILETO_CONTEXT)) {
        (this.brackets as MutableList).addAll(brackets.map { deepCopy() })
    }

    override fun deepCopy(): Bracket = LiletoContext(brackets)

    override fun evaluate() = Companion.evaluate(brackets)

    private fun parse(bracketWithContent: BracketWithContent): Bracket? =
        when(bracketWithContent.bracket) {
            BracketType.LILETO_CONTEXT -> notSupported()
            BracketType.TEXT -> TextContext.parse(bracketWithContent)
            BracketType.SPECIAL_CHAR -> TextContext.parse(bracketWithContent)
            BracketType.COMMENT -> null
            else -> mustBeImplemented("TODO")
        }

    companion object {
        /** @param brackets parsed [Bracket]-s
         * @return evaluated Lileto code */
        fun evaluate(brackets: List<Bracket>) = brackets.joinToString("") { bracket -> bracket.evaluate() }
    }
}

class TextContext(private var text: String) : Bracket() {

    override fun evaluate(): String = text

    override fun deepCopy() = TextContext(text)

    override fun concatenate(other: Bracket): TextContext {
        return when(other) {
            is TextContext -> TextContext(text + other.text)
            else -> notSupported()
        }
    }

    override fun assign(other: Bracket) {
        text = when (other) {
            is TextContext -> other.text
            else -> notSupported()
        }
    }

    companion object {
        fun parse(content: BracketWithContent): TextContext = TextContext(content.toString())
    }

}

class CommandContext(): Bracket() {
    override fun evaluate(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deepCopy(): Bracket {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        fun parse(content: BracketWithContent): CommandContext {
            TODO()
        }
    }
}

class ContainerContext(): Bracket() {
    override fun evaluate(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deepCopy(): Bracket {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class TemplateContext(): Bracket() {
    override fun evaluate(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deepCopy(): Bracket {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        fun parse(content: BracketWithContent): CommandContext {
            TODO()
        }
    }
}

// TODO: Lileto variable registry: mapOf<String, Bracket>()