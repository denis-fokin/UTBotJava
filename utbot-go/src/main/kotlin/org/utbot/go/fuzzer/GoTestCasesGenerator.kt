package org.utbot.go.fuzzer

import org.utbot.go.GoFunctionOrMethodNode
import org.utbot.go.GoFuzzedFunctionOrMethodTestCase

fun generateTestCases(
    functionOrMethodNode: GoFunctionOrMethodNode,
    testSourceRoot: String
): List<GoFuzzedFunctionOrMethodTestCase> = goFuzzing(functionOrMethodNode = functionOrMethodNode)
    .map {
        val executionResult = executeGo(functionOrMethodNode, it, testSourceRoot)
        GoFuzzedFunctionOrMethodTestCase(functionOrMethodNode, it, executionResult)
    }
    .toList()