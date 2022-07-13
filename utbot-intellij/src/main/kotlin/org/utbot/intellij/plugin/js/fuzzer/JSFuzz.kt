package org.utbot.intellij.plugin.js.fuzzer

import com.oracle.js.parser.ir.*
import com.oracle.truffle.api.strings.TruffleString
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.fuzzer.*

fun getTreeConst(
    tree: Expression?,
    lastFuzzedOp: FuzzedOp = FuzzedOp.NONE,
    fuzzedConcreteValues: MutableSet<FuzzedConcreteValue> = mutableSetOf()
): Set<FuzzedConcreteValue> {

    val compOp = """>=|<=|>|<|==|!=""".toRegex()
    val curOp = compOp.find(tree.toString())?.value
    val fuzzedOp = FuzzedOp.values().find { curOp == it.sign } ?: FuzzedOp.NONE
    when (tree) {
        is JoinPredecessorExpression -> {
            getTreeConst((tree.expression as? BinaryNode)?.lhs, fuzzedOp, fuzzedConcreteValues)
            getTreeConst(
                (tree.expression as? BinaryNode)?.rhs,
                fuzzedOp.reverseOrElse { FuzzedOp.NONE },
                fuzzedConcreteValues
            )
        }
        is LiteralNode<*> -> {
            when (tree.value) {
                is TruffleString -> {
                    fuzzedConcreteValues.add(
                        FuzzedConcreteValue(
                            ClassId("<string>"),
                            tree.value.toString(),
                            lastFuzzedOp
                        )
                    )
                }
                is Boolean -> {
                    fuzzedConcreteValues.add(FuzzedConcreteValue(booleanClassId, tree.value, lastFuzzedOp))
                }
                is Integer -> {
                    fuzzedConcreteValues.add(FuzzedConcreteValue(intClassId, tree.value, lastFuzzedOp))
                }
                is Long -> {
                    fuzzedConcreteValues.add(FuzzedConcreteValue(longClassId, tree.value, lastFuzzedOp))
                }
                is Double -> {
                    fuzzedConcreteValues.add(FuzzedConcreteValue(doubleClassId, tree.value, lastFuzzedOp))
                }
            }
        }
        is BinaryNode -> {
            getTreeConst((tree as? BinaryNode)?.lhs, fuzzedOp, fuzzedConcreteValues)
            getTreeConst((tree as? BinaryNode)?.rhs, fuzzedOp.reverseOrElse { FuzzedOp.NONE }, fuzzedConcreteValues)
        }
    }
    return fuzzedConcreteValues
}

fun getIfNodes(node: Node?, IfNodes: MutableList<IfNode> = mutableListOf()): List<IfNode> {
    when (node) {
        is IfNode -> {
            node.fail?.let { getIfNodes(node.fail.statements.first() as? IfNode, IfNodes) }
            node.pass?.let { getIfNodes(node.pass.statements.first() as? IfNode, IfNodes) }
            IfNodes.add(node)
        }
    }
    return IfNodes
}


class JsClassId(private val jsName: String) : ClassId(jsName) {
    override val simpleName: String
        get() = jsName
}


fun jsFuzzing(modelProvider: (ModelProvider) -> ModelProvider = { it }, method: FunctionNode) {
    val execId = MethodId(
        JsClassId("debug"),
        method.name.toString(),
        jsUndefinedClassId,
        method.parameters.toList().map { jsUndefinedClassId }
    )

    val allStatements = mutableListOf<IfNode>()
    method.body.statements.forEach { statement ->
        allStatements.addAll(getIfNodes(statement))
    }
    val allConstants = mutableSetOf<FuzzedConcreteValue>()
    allStatements.forEach {
        getTreeConst(it.test, FuzzedOp.NONE, allConstants)
    }

    val modelProviderWithFallback = modelProvider(ModelProvider.of(JsConstantsModelProvider, JsUndefinedModelProvider))
    val methodUnderTestDescription = FuzzedMethodDescription(execId, allConstants).apply {
        compilableName = method.name.toString()
        val names = method.parameters.map { it.name.toString() }
        parameterNameMap = { index -> names.getOrNull(index) }
    }
    fuzz(methodUnderTestDescription, modelProviderWithFallback).toList()
}
