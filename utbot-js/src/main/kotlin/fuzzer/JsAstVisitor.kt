package fuzzer

import com.oracle.js.parser.ir.BinaryNode
import com.oracle.js.parser.ir.LexicalContext
import com.oracle.js.parser.ir.LiteralNode
import com.oracle.js.parser.ir.Node
import com.oracle.js.parser.ir.visitor.NodeVisitor
import com.oracle.truffle.api.strings.TruffleString
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedOp

object JsAstVisitor : NodeVisitor<LexicalContext>(LexicalContext()) {
    var lastFuzzedOpGlobal = FuzzedOp.NONE
    val fuzzedConcreteValues = mutableSetOf<FuzzedConcreteValue>()

    override fun enterBinaryNode(binaryNode: BinaryNode?): Boolean {
        binaryNode?.let {
            val compOp = """>=|<=|>|<|==|!=""".toRegex()
            val curOp = compOp.find(binaryNode.toString())?.value
            val currentFuzzedOp = FuzzedOp.values().find { curOp == it.sign } ?: FuzzedOp.NONE
            lastFuzzedOpGlobal = currentFuzzedOp
            validateNode(it.lhs)
            lastFuzzedOpGlobal = lastFuzzedOpGlobal.reverseOrElse { FuzzedOp.NONE }
            validateNode(it.rhs)
        }
        return false
    }

    private fun validateNode(literalNode: Node) {
        if (literalNode !is LiteralNode<*>) return
        when (literalNode.value) {
            is TruffleString -> {
                fuzzedConcreteValues.add(
                    FuzzedConcreteValue(
                        jsStringClassId,
                        literalNode.value.toString(),
                        lastFuzzedOpGlobal
                    )
                )
            }
            is Boolean -> {
                fuzzedConcreteValues.add(
                    FuzzedConcreteValue(
                        booleanClassId,
                        literalNode.value,
                        lastFuzzedOpGlobal
                    )
                )
            }
            is Integer -> {
                fuzzedConcreteValues.add(FuzzedConcreteValue(jsIntClassId, literalNode.value, lastFuzzedOpGlobal))
            }
            is Long -> {
                fuzzedConcreteValues.add(FuzzedConcreteValue(jsLongClassId, literalNode.value, lastFuzzedOpGlobal))
            }
            is Double -> {
                fuzzedConcreteValues.add(FuzzedConcreteValue(jsDoubleClassId, literalNode.value, lastFuzzedOpGlobal))
            }
        }
    }
}