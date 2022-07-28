package org.utbot.go.codegen

import org.utbot.framework.plugin.api.GoUtNilModel
import org.utbot.framework.plugin.api.GoUtPrimitiveModel
import org.utbot.framework.plugin.api.util.goStringTypeId
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.fuzzer.FuzzedValue
import org.utbot.go.GoFunctionOrMethodNode
import org.utbot.go.GoFuzzedFunctionOrMethodTestCase
import org.utbot.go.executor.GoUtExecutionCompleted
import org.utbot.go.executor.GoUtExecutionWithNonNullError
import org.utbot.go.executor.GoUtPanicFailure

object GoSimpleCodeGenerator {

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

        // TODO: support methods
        val testCasesByFunctionOrMethod = testCases.groupBy { it.functionOrMethodNode }
        testCasesByFunctionOrMethod.keys.forEach { functionOrMethodNode ->
            val functionOrMethodTestCases = testCasesByFunctionOrMethod[functionOrMethodNode]!!

            functionOrMethodTestCases.filter { !it.isPanicTestCase() }.forEachIndexed { testIndex, testCase ->
                val testFunctionCode = generateTestFunctionForCompletedExecutionTestCase(testCase, testIndex + 1)
                fileBuilder.addTopLevelElements(testFunctionCode)
            }
            functionOrMethodTestCases.filter { it.isPanicTestCase() }.forEachIndexed { testIndex, testCase ->
                val testFunctionCode = generateTestFunctionForPanicFailureTestCase(testCase, testIndex + 1)
                fileBuilder.addTopLevelElements(testFunctionCode)
            }
        }

        return fileBuilder.buildCodeString()
    }

    private fun GoFuzzedFunctionOrMethodTestCase.isPanicTestCase(): Boolean {
        return this.executionResult is GoUtPanicFailure
    }

    private fun generateTestFunctionForCompletedExecutionTestCase(
        testCase: GoFuzzedFunctionOrMethodTestCase,
        testIndex: Int
    ): String {
        val (functionOrMethodNode, fuzzedParametersValues, executionResult) = testCase

        val testFunctionExecutionType =
            if (executionResult is GoUtExecutionWithNonNullError) {
                "WithNonNilError"
            } else {
                ""
            }
        val testFunctionSignatureDeclaration =
            "func Test${functionOrMethodNode.name.capitalize()}${testFunctionExecutionType}ByUtGoFuzzer$testIndex(t *testing.T)"

        val testFunctionBody = if (functionOrMethodNode.returnTypes.isEmpty()) {
            val actualFunctionCall = generateFuzzedFunctionCall(functionOrMethodNode, fuzzedParametersValues)
            "\tassert.NotPanics(t, func() { $actualFunctionCall })"
        } else {
            val bodySb = StringBuilder()

            val returnTypes = functionOrMethodNode.returnTypes
            val isErrorReturnTypes = returnTypes.map { it.isErrorType }
            val actualRvVariablesNames = run {
                val errorVariablesTotal = returnTypes.count { it.isErrorType }
                val commonVariablesTotal = returnTypes.size - errorVariablesTotal

                var errorVariablesIndex = 0
                var commonVariablesIndex = 0
                isErrorReturnTypes.map { isErrorType ->
                    if (isErrorType) {
                        "actualErr${if (errorVariablesTotal > 1) errorVariablesIndex++ else ""}"
                    } else {
                        "actualVal${if (commonVariablesTotal > 1) commonVariablesIndex++ else ""}"
                    }
                }
            }
            val actualFunctionCall = generateFuzzedFunctionCallSavedToVariables(
                actualRvVariablesNames,
                functionOrMethodNode,
                fuzzedParametersValues
            )
            bodySb.append("\t$actualFunctionCall\n\n")

            val expectedModels = (executionResult as GoUtExecutionCompleted).models
            val (assertName, tParameter) = if (expectedModels.size > 1) {
                bodySb.append("\tassertMultiple := assert.New(t)\n")
                "assertMultiple" to ""
            } else {
                "assert" to "t, "
            }
            actualRvVariablesNames.zip(expectedModels).zip(isErrorReturnTypes)
                .forEach { (variableAndModel, isErrorReturnType) ->
                    val (actualRvVariableName, expectedModel) = variableAndModel
                    val assertionMethodCall = if (expectedModel is GoUtNilModel) {
                        "Nil($tParameter$actualRvVariableName)"
                    } else if (isErrorReturnType && expectedModel.classId == goStringTypeId) {
                        "ErrorContains($tParameter$actualRvVariableName, $expectedModel)"
                    } else {
                        "Equal($tParameter$expectedModel, $actualRvVariableName)"
                    }
                    bodySb.append("\t$assertName.$assertionMethodCall\n")
                }
            bodySb.toString()
        }

        return "$testFunctionSignatureDeclaration {\n$testFunctionBody}"
    }

    private fun generateTestFunctionForPanicFailureTestCase(
        testCase: GoFuzzedFunctionOrMethodTestCase,
        testIndex: Int
    ): String {
        val (functionOrMethodNode, fuzzedParametersValues, executionResult) = testCase

        val testFunctionSignatureDeclaration =
            "func Test${functionOrMethodNode.name.capitalize()}PanicsByUtGoFuzzer$testIndex(t *testing.T)"

        val actualFunctionCall = generateFuzzedFunctionCall(functionOrMethodNode, fuzzedParametersValues)
        val functionCallLambda = "func() { $actualFunctionCall }"
        val (expectedModel, originalGoType) = (executionResult as GoUtPanicFailure)
        val testFunctionBody = if (originalGoType.isPrimitive || expectedModel is GoUtNilModel) {
            "\tassert.PanicsWithValue(t, $expectedModel, $functionCallLambda)"
        } else if (originalGoType.isErrorType) { // TODO: improve '== "error"' check
            "\tassert.PanicsWithError(t, $expectedModel, $functionCallLambda)"
        } else {
            "\tassert.Panics(t, $functionCallLambda)"
        }

        return "$testFunctionSignatureDeclaration {\n$testFunctionBody\n}"
    }
}