package org.utbot.go.fuzzer

import org.utbot.go.GoFunctionOrMethodNode
import org.utbot.go.GoFuzzedFunctionOrMethodTestCase
import org.utbot.go.executor.GoExecutor

fun generateTestCases(
    functionOrMethodNode: GoFunctionOrMethodNode
): List<GoFuzzedFunctionOrMethodTestCase> = goFuzzing(functionOrMethodNode = functionOrMethodNode)
    .map { fuzzedParametersValues ->
        val executionResult = GoExecutor.invokeFunctionOrMethod(functionOrMethodNode, fuzzedParametersValues)
        GoFuzzedFunctionOrMethodTestCase(functionOrMethodNode, fuzzedParametersValues, executionResult)
    }
    .toList()