package org.utbot.go

import org.utbot.framework.plugin.api.GoClassId
import org.utbot.framework.plugin.api.GoUtModel
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.go.fuzzer.GoAstVisitor

sealed class GoAstNode {
    @Suppress("UNUSED_PARAMETER")
    fun accept(visitor: GoAstVisitor) {
        // TODO
    }
}

// TODO: remove, it's temporary
data class GoDummyNode(val text: String): GoAstNode()

data class GoFileNode(val name: String) : GoAstNode()

data class GoFunctionOrMethodArgumentNode(val name: String, val type: GoClassId): GoAstNode()

data class GoFunctionOrMethodNode(
    val name: String,
    val returnType: GoClassId,
    val parameters: List<GoFunctionOrMethodArgumentNode>,
    val body: GoAstNode,
    val containingFileNode: GoFileNode
): GoAstNode() {
    val parametersNames get() = parameters.map { it.name }
    val parametersTypes get() = parameters.map { it.type }
}

fun GoFunctionOrMethodNode.toFuzzedMethodDescription(concreteValues: Collection<FuzzedConcreteValue>) =
    FuzzedMethodDescription(name, returnType, parametersTypes, concreteValues).apply {
        compilableName = name
        val names = parametersNames
        parameterNameMap = { index -> names.getOrNull(index) }
    }

data class GoFuzzedFunctionOrMethodTestCase(
    val functionOrMethodNode: GoFunctionOrMethodNode,
    val fuzzedParametersValues: List<FuzzedValue>,
    val executionResultValue: GoUtModel,
)