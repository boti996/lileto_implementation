package boti996.lileto.implementation

import boti996.lileto.implementation.models.Bracket
import boti996.lileto.implementation.models.LiletoContext

@DslMarker
annotation class LiletoDsl


/**
 * It contains all the other brackets
 */
@LiletoDsl
class LiletoContextBuilder {
    private val children = mutableListOf<Bracket>()

    operator fun Bracket.unaryPlus() {
        children += this
    }

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
