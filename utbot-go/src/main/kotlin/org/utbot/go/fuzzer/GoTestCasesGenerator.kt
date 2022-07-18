package org.utbot.go.fuzzer

import org.utbot.go.GoFunctionOrMethodNode
import org.utbot.go.GoFuzzedFunctionOrMethodTestCase

fun generateTestCases(
    functionOrMethodNode: GoFunctionOrMethodNode
): List<GoFuzzedFunctionOrMethodTestCase> = goFuzzing(functionOrMethodNode = functionOrMethodNode)
    .map { fuzzedParametersValues ->
        val executionResult = executeGo(functionOrMethodNode, fuzzedParametersValues)
        GoFuzzedFunctionOrMethodTestCase(functionOrMethodNode, fuzzedParametersValues, executionResult)
    }
    .toList()