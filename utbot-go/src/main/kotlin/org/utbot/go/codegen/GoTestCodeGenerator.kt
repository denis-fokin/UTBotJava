package org.utbot.go.codegen

import org.utbot.go.GoFileNode
import org.utbot.go.GoFuzzedFunctionOrMethodTestCase

@Suppress("UNUSED_PARAMETER")
fun generateTestCode(testCase: GoFuzzedFunctionOrMethodTestCase): String {
    TODO()
}

fun generateTestsFilesAndCode(testCasesByFile: Map<GoFileNode, List<GoFuzzedFunctionOrMethodTestCase>>) {
    testCasesByFile.keys.forEach { fileNode ->
        val fileTestsString =
            testCasesByFile[fileNode]!!
                .joinToString(separator = "\n\n") { generateTestCode(it) } // TODO: package and imports
        println("GENERATED TEST FOR FILE ${fileNode.name}:\n")
        println(fileTestsString) // TODO: create file and write each test into it
    }
}