package org.utbot.intellij.plugin.js.codegen

import com.oracle.js.parser.ir.FunctionNode
import org.utbot.framework.plugin.api.JsUtModel
import org.utbot.fuzzer.FuzzedValue

object JsTestCodeGenerator {

    fun generateTestCode(
        method: FunctionNode,
        params: List<FuzzedValue>,
        returnValue: JsUtModel,
    ): String {
        val testFunctionName = "${method.name}"
        val testFunctionTitle = "function $testFunctionName() {"
//        val arguments = execution.stateBefore.parameters.zip(method.arguments).map { (model, argument) ->
//            "${argument.name} = $model"
//        }
//        val functionArguments = method.arguments.map { argument ->
//            "${argument.name}=${argument.name}"
//        }
//        val actualName = "actual"
//        val functionCall = listOf("$actualName = ${method.name}(") +
//                addIndent(functionArguments.map {
//                    "$it,"
//                }) +
//                listOf(")")
//
//        val correctResultName = "correct_result"
//        val correctResult = "$correctResultName = ${execution.result}"
//        val assertLine = "assert $actualName == $correctResultName"
//
//        val codeRows = arguments + functionCall + listOf(correctResult, assertLine)
//        val functionRows = listOf(testFunctionTitle) + addIndent(codeRows)
//        return functionRows.joinToString("\n")
        return ""
    }
}