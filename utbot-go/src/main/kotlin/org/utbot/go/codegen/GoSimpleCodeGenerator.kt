package org.utbot.go.codegen

import org.utbot.common.unreachableBranch
import org.utbot.framework.plugin.api.GoUtModel
import org.utbot.framework.plugin.api.GoUtPrimitiveModel
import org.utbot.framework.plugin.api.nullableToGoUtModel
import org.utbot.framework.plugin.api.util.goBoolClassId
import org.utbot.framework.plugin.api.util.goStringClassId
import org.utbot.fuzzer.FuzzedValue
import org.utbot.go.GoFunctionOrMethodNode
import org.utbot.go.GoFuzzedFunctionOrMethodTestCase
import org.utbot.go.executor.GoUtExecutionCompleted
import org.utbot.go.executor.GoUtPanicFailure

object GoSimpleCodeGenerator {

    object Constants {
        const val PANIC_CHECKER_FUNCTION_NAME = "checkPanicByUtGo"
    }

    class GoFileCodeBuilder {

        private var packageLine: String? = null
        private var importLines: String? = null
        private val topLevelElements: MutableList<String> = mutableListOf()

        fun buildCodeString(): String {
            return "$packageLine\n\n$importLines\n\n${topLevelElements.joinToString(separator = "\n\n")}"
        }

        fun setPackage(packageName: String) {
            packageLine = "package $packageName"
        }

        fun setImports(importNames: List<String>) {
            if (importNames.isEmpty()) return
            if (importNames.size == 1) {
                importLines = "import ${importNames.first()}"
                return
            }
            importLines = importNames.joinToString(separator = "", prefix = "import(\n", postfix = ")") {
                "\t\"$it\"\n"
            }
        }

        fun addTopLevelElements(vararg originalElements: String) {
            topLevelElements.addAll(originalElements)
        }
    }

    fun generateFuzzedFunctionCallSavedToVariables(
        variablesNames: List<String>,
        functionOrMethodNode: GoFunctionOrMethodNode,
        fuzzedParametersValues: List<FuzzedValue>
    ): String = generateVariablesDeclarationTo(
        variablesNames,
        generateFuzzedFunctionCall(functionOrMethodNode, fuzzedParametersValues)
    )

    fun generateVariablesDeclarationTo(variablesNames: List<String>, expressionCode: String): String {
        val variables = variablesNames.joinToString(separator = ", ")
        return "$variables := $expressionCode"
    }

    fun generateFuzzedFunctionCall(
        functionOrMethodNode: GoFunctionOrMethodNode,
        fuzzedParametersValues: List<FuzzedValue>
    ): String {
        val fuzzedParameters =
            fuzzedParametersValues.joinToString(separator = ", ") {
                (it.model as GoUtPrimitiveModel).toString()
            }
        return "${functionOrMethodNode.name}($fuzzedParameters)"
    }

    fun generateTestFileCode(testCases: List<GoFuzzedFunctionOrMethodTestCase>): String {
        val fileBuilder = GoFileCodeBuilder()

        val packageName = testCases.first().containingPackagePath
        if (testCases.any { it.containingPackagePath != packageName }) {
            error("Test cases of the file corresponds to different packages.")
        }
        fileBuilder.setPackage(packageName)

        fileBuilder.setImports(listOf("testing", "github.com/stretchr/testify/assert"))

        val testCasesByFunctionOrMethod = testCases.groupBy { it.functionOrMethodNode.name }
        testCasesByFunctionOrMethod.keys.forEach {
            val functionOrMethodTestCases = testCasesByFunctionOrMethod[it]!!
            for (testCase in functionOrMethodTestCases) {
                val testFunctionCode = generateTestFunctionForTestCase(testCase)
                fileBuilder.addTopLevelElements(testFunctionCode)
            }
        }

        val panicCheckerFunctionDeclaration = """
            func ${Constants.PANIC_CHECKER_FUNCTION_NAME}(action func()) (panicked bool, panicMessage any) {
            	defer func() {
            		panicMessage = recover()
            	}()
                panicked = true
            	action()
            	panicked = false
            	return
            }
        """.trimIndent()
        fileBuilder.addTopLevelElements(panicCheckerFunctionDeclaration)

        return fileBuilder.buildCodeString()
    }

    // TODO: handle methods case
    private fun generateTestFunctionForTestCase(testCase: GoFuzzedFunctionOrMethodTestCase): String {

        val (actualRvVariablesNames, actualFunctionCallSavedToVariablesCode, expectedModels) =
            when (testCase.executionResult) {
                is GoUtPanicFailure -> generateFuzzedFunctionCallAndRvVariablesForPanicFailure(testCase)
                is GoUtExecutionCompleted -> generateFuzzedFunctionCallAndRvVariablesForCompletedExecution(testCase)
                else -> unreachableBranch("No other implementations of GoUtExecutionResult are known yet.")
            }

        val assertions = actualRvVariablesNames.zip(expectedModels)
            .joinToString(separator = "\n") { (actualRvVariableName, expectedModel) ->
                "assert.Equal(t, $expectedModel, $actualRvVariableName)"
            }

        return "{\n$actualFunctionCallSavedToVariablesCode\n\n$assertions\n}"
    }

    private data class TestFunctionCodeData(
        val actualRvVariablesNames: List<String>,
        val actualFunctionCallSavedToVariablesCode: String,
        val expectedModels: List<GoUtModel>
    )

    private fun generateFuzzedFunctionCallAndRvVariablesForCompletedExecution(
        testCase: GoFuzzedFunctionOrMethodTestCase
    ): TestFunctionCodeData {
        val (functionOrMethodNode, fuzzedParametersValues, executionResult) = testCase

        val actualRvVariablesNames = run {
            var errorVariablesCnt = 0
            var commonVariablesCnt = 0

            functionOrMethodNode.returnCommonTypes.map { returnType ->
                if (returnType.isErrorType) {
                    "actualErr${errorVariablesCnt++}"
                } else {
                    "actualVal${commonVariablesCnt++}"
                }
            }
        }

        val fuzzedFunctionCall = generateFuzzedFunctionCallSavedToVariables(
            actualRvVariablesNames,
            functionOrMethodNode,
            fuzzedParametersValues
        )

        val expectedModels = (executionResult as GoUtExecutionCompleted).models

        return TestFunctionCodeData(actualRvVariablesNames, fuzzedFunctionCall, expectedModels)
    }

    private fun generateFuzzedFunctionCallAndRvVariablesForPanicFailure(
        testCase: GoFuzzedFunctionOrMethodTestCase
    ): TestFunctionCodeData {
        val (functionOrMethodNode, fuzzedParametersValues, executionResult) = testCase

        val actualRvVariablesNames = listOf("panicked", "panicMessage")

        val functionWrappedInPanicCheckerCall = """
            ${Constants.PANIC_CHECKER_FUNCTION_NAME}(func() {
                ${generateFuzzedFunctionCall(functionOrMethodNode, fuzzedParametersValues)}
            })
        """.trimIndent()
        val wrappedFuzzedFunctionCall =
            generateVariablesDeclarationTo(actualRvVariablesNames, functionWrappedInPanicCheckerCall)

        val expectedModels = listOf(
            GoUtPrimitiveModel(true, goBoolClassId),
            nullableToGoUtModel((executionResult as GoUtPanicFailure).failureMessage, goStringClassId)
        )

        return TestFunctionCodeData(actualRvVariablesNames, wrappedFuzzedFunctionCall, expectedModels)
    }
}