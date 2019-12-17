package boti996.lileto.implementation.parser

import boti996.lileto.implementation.parser.BracketType.COMMAND
import java.util.*

/**
 * A storage for containing a lanced list of binary operations
 * Examples of usage:
 * "b" --> Operation(null, "b", null)
 * "a=b" --> Operation("=", "a", Operation(null, "b", null))
 * "a=b+c" --> Operation("=", "a", Operation("+", "b", "c"))
 * "b+c+d" --> Operation("+", "b", Operation("<", "c", "d"))
 */
data class Operation(var op: String? = null, var left: String? = null, var right: Operation? = null) {
    override fun toString(): String {
        return "$left$op$right"
    }
}

object CommandParser {

    enum class OperatorTypes(val pattern: String, val value: List<String>) {
        ASSIGN("\\=", listOf("=")),
        DECLARE("\\:$ws${word("type", true)}$ws\\=", listOf(":", "=")),
        CONCATENATE("\\+"),
        INSERT("\\<");

        val regex = Regex(this.pattern)
    }
    // Regex building patterns
    private fun word(name: String = "", canBeBlank: Boolean = false)
            = "(${name.let { if (it.isBlank()) "?:" else "?<$name>" }}\\w" +    // non-capturing group without name parameter
            "${canBeBlank.let { if (canBeBlank) "*" else "+" }})"               // word can be zero-length
    // whitespaces
    private const val ws = "\\s*"
    // separator
    private const val sep = "(?:\\s+|,|;)"
    // general operator
    private fun operator(name: String, operators: List<String>) = "(?<$name>${operators.joinToString("|")})"
    // assign or declaration operator
    private val assigns = operator("assign", listOf(OperatorTypes.DECLARE.pattern, OperatorTypes.ASSIGN.pattern))
    // concatenate or insert operator
    private val otherOperands = operator("operator", listOf(OperatorTypes.CONCATENATE.pattern, OperatorTypes.INSERT.pattern))
    // binary operator
    private val fullOperation =
        "${word("variable")}$ws$assigns$ws" +
                "${word("left")}$ws" +
                "(?:$otherOperands$ws${word("right")}$ws)*$sep*$ws"
    // name capturing
    private val name = "^$ws=$ws${word("name")}$ws" + "$sep$ws"
    // output capturing
    private val output = "(?<outp>${ws}outp$ws=$ws${word("out")}$ws$)"
    // all operations - TODO: should be use this instead of (?:.*) non-capturing group -> we get trimmedInput explicitly
    private val ops = "(?:$fullOperation)*"

    // Enclosed return value
    private data class NameOutputEvaluationResult(val evaluation: CommandEvaluationResult, val newInput: String)
    // Return evaluated name variable, output variable and trimmed input contains only the operations
    private fun evaluateNameOutput(input: String): NameOutputEvaluationResult {
        val fullCommandPattern = Regex("$name(.*)$fullOperation$output")

//        println(input)
        // TODO: test cases when there's no name or output
        // TODO: output's right side should be an operation
        val result = fullCommandPattern.find(input) ?: throw IllegalArgumentException() // TODO: full check before

        val nameGroup = result.groups[1]
        val outputGroup = result.groups[result.groups.size - 2]

        val beginOfOutput = outputGroup?.range?.first ?: TODO("This should not be null")
        val endOfName = nameGroup?.range?.last ?: TODO("This should not be null")
        val trimmedInput = input
            .take(beginOfOutput)
            .takeLast((input.length-1) - endOfName)

        return NameOutputEvaluationResult(CommandEvaluationResult(nameGroup.value, Operation(left = outputGroup.value)), trimmedInput)
    }

    // Indices used during parsing
    private enum class GroupIndex(val value: Int) { VAR(1), ASSIGN(2), TYPE(3), OP_LEFT(4), OPERATOR(5), OP_RIGHT(6)}

    private fun evaluateOperations(preParsingResults: NameOutputEvaluationResult): CommandEvaluationResult {
        var (evaluation, input) = preParsingResults

        var parsedOperations = evaluation.operations
        val operandParsingStack = Stack<Operation>()

        val singleOperationPattern = Regex("(?:.*)$fullOperation${'$'}")

        while (input.isNotBlank()) {
            // Get the rightmost operation
            println(input)
            var currentOperation = singleOperationPattern.find(input) ?: throw IllegalArgumentException()
            var groups = currentOperation.groups
            assert(currentOperation.groupValues.size == groups.size)
            assert(groups.size == GroupIndex.OP_RIGHT.value)

            // Parse rightmost operand with it's operation
            val rightmostOperand = groups[GroupIndex.OP_RIGHT.value]?.value ?: break
            while (rightmostOperand.isNotBlank()) {
                // Register operation
                val rightmostOperation = Operation(op = groups[GroupIndex.OPERATOR.value]?.value ?: break)
                // Found the very last operand
                if (operandParsingStack.empty()) {
                    // Register right operand
                    rightmostOperation.right = Operation(left =groups[GroupIndex.OP_RIGHT.value]?.value ?: break)
                    // Otherwise
                } else {
                    // Register previous operation's left operand
                    val operationOnTheLeft = operandParsingStack.peek()
                    operationOnTheLeft.left = groups[GroupIndex.OP_RIGHT.value]?.value ?: break
                    // Register left operand
                    rightmostOperation.right = operandParsingStack.peek()
                }
                // Store it to use it later as the next operation's left operand
                operandParsingStack.push(rightmostOperation)
                // Trim last operand and last operator
                input = input.take(groups[GroupIndex.OPERATOR.value]?.range?.first ?: TODO())
                // Get the rightmost operation after trimming
                println(input)
                currentOperation = singleOperationPattern.find(input) ?: throw IllegalArgumentException()
                groups = currentOperation.groups
            }

            // There were operations other than the assign operation
            if (operandParsingStack.isNotEmpty()) {
                // Add assigned variable as the left side of last operation
                operandParsingStack.peek().left = groups[GroupIndex.OP_LEFT.value]?.value ?: break
            }
            // Register assign operation
            parsedOperations.add(
                Operation(op = groups[GroupIndex.ASSIGN.value]?.value ?: break,
                    left = groups[GroupIndex.VAR.value]?.value ?: break,
                    right = operandParsingStack.lastOrNull()))
            // Trim current operation
            input = input.take(currentOperation.groups[1]?.range?.first ?: break)
            //
            operandParsingStack.clear()
        }

        evaluation.operations = evaluation.operations.asReversed()

        println(evaluation.operations)
        return evaluation
    }

    // Enclosed return values
    data class CommandEvaluationResult(val name: String, val output: Operation, var operations: MutableList<Operation> = mutableListOf())
    /** @return evaluated lance of operations */
    fun evaluateCommands(command: BracketWithContent): CommandEvaluationResult {
        if (command.bracket != COMMAND) throw TODO("This should be a command bracket")

        // Create a string. If there are brackets in it, use "_${object hash code}" as their Lileto variable name mock
        val originalInput = command.content
            .joinToString("") { if(it is BracketWithContent) it.getTemporaryRegistryName() else it.toString() } // TODO: check it's correctness in practice

        try {
            return evaluateOperations(evaluateNameOutput(originalInput))
        } catch (e: IllegalArgumentException) { throw IllegalArgumentException(     // Exception builder function + doument KDoc @throws tags
            "There was a problem during parsing " +
                    "in command bracket with the following content:\n" +
                    "line:${command.userErrorInfo.line}:${command.userErrorInfo.column}\t-->\t\t${command.content}") }
    }
}

//TODO: test with no operations input
//val (name, output, operations)
//        = CommandEvaluation.evaluateCommands(
//    boti996.lileto.implementation.parser.BracketWithContent(
//        boti996.lileto.implementation.parser.BracketType.COMMAND,
//        kotlin.collections.mutableListOf("=name a=b+c+d+e+f g=h+i+j outp=out")
//    )
//)
