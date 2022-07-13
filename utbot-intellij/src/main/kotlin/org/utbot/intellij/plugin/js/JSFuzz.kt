package org.utbot.intellij.plugin.js

import com.oracle.js.parser.ir.*
import com.oracle.truffle.api.strings.TruffleString
import org.utbot.engine.isMethod
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.fuzzer.*
import org.utbot.fuzzer.providers.ConstantsModelProvider
import org.utbot.fuzzer.providers.PrimitivesModelProvider

private val set = mutableSetOf<FuzzedConcreteValue>()


fun getTreeConst(tree: Expression?, lastFuzzedOp: FuzzedOp = FuzzedOp.NONE): Set<FuzzedConcreteValue>? {


    val regex = """>|<|>=|<=|==|!=""".toRegex()

    if (tree == null) {
        return null
    }
    val b = tree
    when (tree) {
        is JoinPredecessorExpression -> {
            println((tree.expression as? BinaryNode).toString())
            val currentOperation = regex.find((tree.expression as? BinaryNode).toString())?.value
            var fuzzedOp = FuzzedOp.NONE
            for (el in FuzzedOp.values()) {
                if (currentOperation == el.sign)
                    fuzzedOp = el
            }
            getTreeConst((tree.expression as? BinaryNode)?.lhs, fuzzedOp)
            getTreeConst((tree.expression as? BinaryNode)?.rhs, fuzzedOp)
        }
        is IdentNode -> {
            //TODO
        }
        is LiteralNode<*> -> {
            when (tree.value) {
                is TruffleString -> {
                    set.add(
                        FuzzedConcreteValue(
                            ClassId("<string>"),
                            tree.value.toString(),
                            lastFuzzedOp
                        )
                    )
                }
                is Boolean -> {
                    set.add(FuzzedConcreteValue(booleanClassId, tree.value, lastFuzzedOp))
                }
                is Integer -> {
                    set.add(FuzzedConcreteValue(intClassId, tree.value, lastFuzzedOp))
                }
                is Double -> {
                    set.add(FuzzedConcreteValue(doubleClassId, tree.value, lastFuzzedOp))
                }
            }
        }
        else -> {
            println((tree as BinaryNode).toString())
            getTreeConst((tree as? BinaryNode)?.lhs)
            getTreeConst((tree as? BinaryNode)?.rhs)
        }
    }
    return set
}


class JsClassId(val jsName: String): ClassId(jsName) {
    override val simpleName: String
        get() = jsName
}

val undefinedClassId = JsClassId("undefined")
private var nextDefaultModelId = 1500_000_000

fun jsFuzzing(modelProvider: (ModelProvider) -> ModelProvider = { it }, method: FunctionNode) {
    val execId = MethodId(
        JsClassId("debug"),
        method.name.toString(),
        undefinedClassId,
        method.parameters.toList().map { undefinedClassId }
    )
    method.body.statements.forEach {
        when(it) {
            is IfNode -> {
                getTreeConst(it.test as BinaryNode)
            }
        }
    }
    val modelProviderWithFallback = modelProvider(ModelProvider.of(ConstantsModelProvider))
    val methodUnderTestDescription = FuzzedMethodDescription(execId, set).apply {
        compilableName = method.name.toString()
        val names = method.parameters.map {it.name.toString()}
        parameterNameMap = { index -> names.getOrNull(index) }
    }
    val res = fuzz(methodUnderTestDescription, modelProviderWithFallback).toList()
    val k = 1
}