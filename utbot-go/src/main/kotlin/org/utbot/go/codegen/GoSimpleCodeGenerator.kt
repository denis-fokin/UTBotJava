package org.utbot.go.codegen

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

        val packageName = testCases.first().containingPackageName
        if (testCases.any { it.containingPackageName != packageName }) {
            error("Test cases of the file corresponds to different packages.")
        }
        fileBuilder.setPackage(packageName)

        fileBuilder.setImports(listOf("github.com/stretchr/testify/assert", "testing"))

        val testCasesByFunctionOrMethod = testCases.groupBy { it.functionOrMethodNode }
        testCasesByFunctionOrMethod.keys.forEach {
            val functionOrMethodTestCases = testCasesByFunctionOrMethod[it]!!
            functionOrMethodTestCases.forEachIndexed { testIndex, testCase ->
                val testFunctionCode = generateTestFunctionForTestCase(testCase, testIndex)
                fileBuilder.addTopLevelElements(testFunctionCode)
            }
        }

        if (testCases.any { it.isPanicTestCase() }) {
            val panicCheckerFunctionDeclaration = """
                func ${Constants.PANIC_CHECKER_FUNCTION_NAME}(action func()) (panicked bool, panicMessage any) {
                    defer func() {
                        panicMessage = recover()
                    }()
                    panicked = true
                    action()
                    panicked = false
                    return
                }""".trimIndent()
            fileBuilder.addTopLevelElements(panicCheckerFunctionDeclaration)
        }

        return fileBuilder.buildCodeString()
    }

    // TODO: handle methods case
    private fun generateTestFunctionForTestCase(testCase: GoFuzzedFunctionOrMethodTestCase, testIndex: Int): String {
        val testFunctionSignatureDeclaration =
            "func Test${testCase.functionOrMethodNode.name.capitalize()}ByUtGoFuzzer$testIndex(t *testing.T)"

        val (actualRvVariablesNames, actualFunctionCallSavedToVariablesCode, expectedModels) =
            if (testCase.isPanicTestCase()) {
                generateFuzzedFunctionCallAndRvVariablesForPanicFailure(testCase)
            } else {
                generateFuzzedFunctionCallAndRvVariablesForCompletedExecution(testCase)
            }

        val assertions = actualRvVariablesNames.zip(expectedModels)
            .joinToString(separator = "\n") { (actualRvVariableName, expectedModel) ->
                "\tassert.Equal(t, $expectedModel, $actualRvVariableName)"
            }

        return "$testFunctionSignatureDeclaration {\n\t$actualFunctionCallSavedToVariablesCode\n\n$assertions\n}"
    }

    private fun GoFuzzedFunctionOrMethodTestCase.isPanicTestCase(): Boolean {
        return this.executionResult is GoUtPanicFailure
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
            val returnTypes = functionOrMethodNode.returnCommonTypes
            val errorVariablesTotal = returnTypes.count { it.isErrorType }
            val commonVariablesTotal = returnTypes.size - errorVariablesTotal

            var errorVariablesIndex = 0
            var commonVariablesIndex = 0
            functionOrMethodNode.returnCommonTypes.map { returnType ->
                if (returnType.isErrorType) {
                    "actualErr${if (errorVariablesTotal > 1) errorVariablesIndex++ else ""}"
                } else {
                    "actualVal${if (commonVariablesTotal > 1) commonVariablesIndex++ else ""}"
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