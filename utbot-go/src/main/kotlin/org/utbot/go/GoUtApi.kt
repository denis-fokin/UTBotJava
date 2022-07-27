package org.utbot.go

import org.utbot.framework.plugin.api.GoClassId
import org.utbot.framework.plugin.api.GoCommonClassId
import org.utbot.framework.plugin.api.GoSyntheticClassesTupleId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.go.executor.GoUtExecutionResult
import org.utbot.go.fuzzer.GoAstVisitor

sealed class GoAstNode {
    @Suppress("UNUSED_PARAMETER")
    fun accept(visitor: GoAstVisitor) {
        // TODO
    }
}

data class GoFileNode(
    val name: String,
    val containingPackageName: String,
    val containingPackagePath: String
) : GoAstNode()

data class GoBodyNode(val text: String) : GoAstNode()

data class GoFunctionOrMethodParameterNode(val name: String, val type: GoClassId) : GoAstNode()

data class GoFunctionOrMethodNode(
    val name: String,
    val returnType: GoClassId,
    val parameters: List<GoFunctionOrMethodParameterNode>,
    val body: GoBodyNode,
    val containingFileNode: GoFileNode
) : GoAstNode() {
    val parametersNames get() = parameters.map { it.name }
    val parametersTypes get() = parameters.map { it.type }

    val returnCommonTypes: List<GoCommonClassId>
        get() = if (returnType is GoSyntheticClassesTupleId) {
            returnType.classes
        } else {
            listOf(returnType as GoCommonClassId)
        }
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
    val executionResult: GoUtExecutionResult,
) {
    val containingPackageName: String get() = functionOrMethodNode.containingFileNode.containingPackageName
}