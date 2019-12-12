import boti996.lileto.implementation.parser.plaintextToLiletoParser
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val params = MyArgumentParser.parseArguments(args)
    val inputPath = params["in"] as String
    val outputPath = params["out"] as String

    val inputString = File(inputPath).inputStream().readBytes().toString(Charsets.UTF_8)
    params.printInput(inputString)

    val outputString = plaintextToLiletoParser(inputString)
    params.printOutput(outputString)

    File(outputPath).writeText(outputString)
    println("Output was generated successfully")
}

typealias Arguments = Map<String, Any>

fun Arguments.printInput(input: String) {
    this["print-in"]?.let {
        println("Input:")
        println(input)
    }
}

fun Arguments.printOutput(output: String) {
    this["print-out"]?.let {
        this["print-in"]?.let { println() }
        println("Output:")
        println(output) }
}

data class Argument(val name: String, val short: String? = null, val description: String,
                    val isRequired: Boolean = true, val isFlag: Boolean = false)

object MyArgumentParser {

    fun parseArguments(args: Array<String>): Arguments {
        val flags = listOf(
            Argument("--in", "-i", "Input file path. File should be in .llt format"),
            Argument("--out", "-o", "Output file path."),
            Argument("--helper", "-h", "Show this helper and quit.", isRequired = false, isFlag = true),
            Argument("--print-in", description = "Print input to stdout.", isRequired = false, isFlag = true),
            Argument("--print-out", description = "Print output to stdout.", isRequired = false, isFlag = true))

        showHelp(args, flags)

        val helpExceptionMessage = "Please use --help flag to see the correct usages."

        for (flag in flags) {
            require(!(flag.isRequired && !args.any { it in setOf(flag.name, flag.short) })) {
                "${flag.name} is missing. $helpExceptionMessage"
            }
        }

        var skipParameterParsing = false
        val parameters = mutableMapOf<String, Any>()
        for (arg in args) {
            if (skipParameterParsing) {
                skipParameterParsing = false
                continue
            }

            val flag = flags.filter { it.name == arg || it.short == arg }.firstOrNull()
                ?: throw IllegalArgumentException("$arg is an invalid parameter. $helpExceptionMessage")

            val paramName = flag.name.replace("--", "")

            if (!flag.isFlag) {
                val paramIndex = args.indexOf(arg) + 1
                parameters[paramName] = args[paramIndex]
                skipParameterParsing = true
            } else {
                parameters[paramName] = true
            }
        }

        return parameters
    }

    private fun showHelp(args: Array<String>, flags: List<Argument>) {
        if (args.any{it in setOf("--help", "-h")}) {
            val builder = StringBuilder("Usage: ")
            for (flag in flags) {
                if (!flag.isRequired) {
                    builder.append("[")
                }
                builder.append(flag.name)
                if (!flag.isFlag) {
                    builder.append(" <param> ")
                }
                if (!flag.isRequired) {
                    builder.append("]")
                }
            }
            println(builder.toString())

            for (flag in flags) {
                println(flag.name + " " + flag.short + " : " + flag.description)
            }

            exitProcess(0)
        }
    }


}
