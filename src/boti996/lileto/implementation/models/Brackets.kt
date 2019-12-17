package boti996.lileto.implementation.models

import boti996.lileto.implementation.parser.BracketType
import boti996.lileto.implementation.parser.BracketWithContent
import boti996.lileto.implementation.parser.CommandParser
import boti996.lileto.implementation.parser.Operation
import java.lang.IllegalArgumentException


fun notSupported() : Nothing = throw IllegalStateException("This operation is not supported")
fun mustBeImplemented(message: String) : Nothing = throw NotImplementedError(message)   // TODO: debug only


/**
 * Registry for accessing local Lileto variables
 */
class Registry {
    private val registry = mutableMapOf<String, Bracket>()
    /** @param variableName Lileto variable's name
     * @return Lileto variable's value */
    operator fun get(variableName: String): Bracket? = registry[variableName]
    /** @param variableName Lileto variable's name
     * @param bracket Lileto variable's value
     * Register Lileto variable */
    operator fun set(variableName: String, bracket: Bracket): Bracket = bracket
        .also { registry[variableName] = bracket }
    /** @param variableName Lileto variable's name
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
    private val localRegistry = Registry()
    /** Visible registries */
    private val scopeRegistry = mutableListOf(localRegistry)
    /** @param scopeRegistry visible registries in current scope
     * Add registries to [scopeRegistry] field */
    operator fun plusAssign(scopeRegistry: List<Registry>) { this.scopeRegistry.addAll(scopeRegistry) }
    /** @param scopeRegistry visible registries in current scope
     * Add registry to [scopeRegistry] field */
    operator fun plusAssign(scopeRegistry: Registry) { this.scopeRegistry.add(scopeRegistry) }
    /** @param variableName Lileto variable's name
     * @return Lileto variable''s vaue from [scopeRegistry] */
    operator fun get(variableName: String): Bracket? {
        scopeRegistry.forEach { registry -> registry[variableName]?.let { return it } }
        return null
    }
    /** @param variableName Lileto variable's name
     *  @param bracket Lileto variable's value
     *  Register Lileto variable to [localRegistry] */
    operator fun set(variableName: String, bracket: Bracket): Bracket = localRegistry.set(variableName, bracket)

    /** @return evaluated Lileto code */
    abstract fun evaluate(): String
    /** @param other
     * @return deep copy of concatenated content in a new object */
    open fun concatenate(other: Bracket): Bracket = notSupported()
    /** @param other
     * Initialize current object with [other]'s content */
    open fun assign(other: Bracket): Bracket = notSupported()
    /** @param other
     * @return deep copy of current object's content modified with [other]'s content in a new object */
    open fun insert(other: Bracket): Bracket = notSupported()

    /** @return deep copy of current object's content in a new object  */
    abstract fun deepCopy(): Bracket

    companion object {
        /** Global Lileto registry
         *  In current version every variable can be reachable here, but only via bracket namespaces */
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
            // TODO: add remaining types
            else -> mustBeImplemented("TODO")
        }?.also { it += globalRegistry }

    companion object {
        /** @param brackets parsed [Bracket]-s
         * @return evaluated Lileto code */
        fun evaluate(brackets: List<Bracket>) = brackets.joinToString("") { bracket -> bracket.evaluate() }
    }
}

// TODO: make it with decorator instead of an interface
interface CommandContent

class TextContext(private var text: String) : Bracket(), CommandContent {

    override fun evaluate(): String = text

    override fun deepCopy() = TextContext(text)

    override fun concatenate(other: Bracket): TextContext {
        return when(other) {
            is TextContext -> TextContext(text + other.text)
            else -> notSupported()
        }
    }

    override fun assign(other: Bracket): TextContext {
        text = when (other) {
            is TextContext -> other.text
            else -> notSupported()
        }
        return this
    }

    companion object {
        fun parse(content: BracketWithContent): TextContext = TextContext(content.toString())
    }
}

private fun Bracket.registerToGlobal(name: String) { if (name.isNotBlank()) Bracket.globalRegistry[name] = this }

data class CommandContext(val name: String = "", val output: Operation, val operations: List<Operation>): Bracket(), CommandContent {
    init { registerToGlobal(name) }

    private constructor(result: CommandParser.CommandEvaluationResult): this(result.name, result.output, result.operations)

    override fun evaluate(): String {
        return if (output.left!!.isNotBlank()) {
            val outputBracket = output.evaluate()
            outputBracket.evaluate()
        } else ""
    }

    override fun deepCopy(): Bracket {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // TODO: print the place of exception for user
    private fun Operation.evaluate(): Bracket {
        // Operation
        this@evaluate.op?.let {operator ->
            // TODO: refactor reduntant parts
            // Assign value to an existing Lileto variable
            return if (operator.matches(CommandParser.OperatorTypes.ASSIGN.regex)) {

                val variableToAssign = this@CommandContext[this@evaluate.left ?: mustBeImplemented("Operation's left operand must never be empty here")]
                    ?: throw IllegalArgumentException("Tried to assign a non-existent variable")

                val assignedValue = this@evaluate.right?.evaluate() ?: mustBeImplemented("Operation's right operand must never be empty here")

                variableToAssign.assign(assignedValue)
            // Declare a new Lileto variable
            } else if (operator.matches(CommandParser.OperatorTypes.DECLARE.regex)) {
                // TODO: type-checking: throws runtime-error in current version
                val declarationValue = this@evaluate.right?.evaluate() ?: mustBeImplemented("Operation's right operand must never be empty here")

                val variableName = this@evaluate.left ?: mustBeImplemented("Operation's left operand must never be empty here")

                this@CommandContext.set(variableName, declarationValue.deepCopy())
            // Concatenate Lileto variables
            } else if (operator.matches(CommandParser.OperatorTypes.CONCATENATE.regex)) {
                val leftValue = this@CommandContext[this@evaluate.left
                    ?: mustBeImplemented("Operation's left operand must never be empty here")]
                    ?: throw IllegalArgumentException("Tried to concatenate to a non-existent variable")

                val rightValue = this@evaluate.right?.evaluate()
                    ?: mustBeImplemented("Operation's right operand must never be empty here")
                    ?: throw IllegalArgumentException("Tried to concatenate a non-existent variable")

                leftValue.concatenate(rightValue)
            // Insert into Lileto variable
            } else if (operator.matches(CommandParser.OperatorTypes.INSERT.regex)) {
                val leftValue =  this@CommandContext[this@evaluate.left ?: mustBeImplemented("Operation's left operand must never be empty here")]
                    ?: throw IllegalArgumentException("Tried to concatenate to a non-existent variable")

                val rightValue = this@evaluate.right?.evaluate() ?: mustBeImplemented("Operation's right operand must never be empty here")
                ?: throw IllegalArgumentException("Tried to concatenate a non-existent variable")

                leftValue.insert(rightValue)
            } else mustBeImplemented("There should not be unknown operators in the pre-parsed content")
        }
        // Single value NOTE: unary operators might come here in the future
        return this@CommandContext[this@evaluate.left ?: mustBeImplemented("TODO")]
            ?: throw IllegalArgumentException("Tried to get non-existent variable")
    }

    companion object {
        fun parse(command: BracketWithContent): CommandContext {
            val commandContext = CommandContext(CommandParser.evaluateCommands(command))
            // Create inner brackets
            command.content.forEach { content ->
                if (content is BracketWithContent) {

                    @Suppress("IMPLICIT_CAST_TO_ANY")
                    commandContext[content.getTemporaryRegistryName()] =
                        (when (content.bracket) {
                            BracketType.TEXT -> TextContext.parse(content)
                            BracketType.COMMAND -> parse(content)
                            BracketType.CONTAINER -> ContainerContext.parse(content)
                            BracketType.TEMPLATE -> TextContext.parse(content)
                            else -> notSupported()
                        } as Bracket)
                            // Register inner brackets into local registry
                            .also { bracket -> commandContext[content.getTemporaryRegistryName()] = bracket }
                }
            }

            return commandContext
        }
    }
}

class ContainerContext(name: String = ""): Bracket(), CommandContent {
    init { registerToGlobal(name) }

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

class TemplateContext(name: String = ""): Bracket(), CommandContent {
    init { registerToGlobal(name) }

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
