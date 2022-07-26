package org.utbot.go.executor

import mu.KotlinLogging
import org.utbot.common.pid
import org.utbot.framework.plugin.api.GoCommonClassId
import org.utbot.framework.plugin.api.GoUtNullModel
import org.utbot.framework.plugin.api.GoUtPrimitiveModel
import org.utbot.framework.plugin.api.util.goStringClassId
import org.utbot.fuzzer.FuzzedValue
import org.utbot.go.GoFunctionOrMethodNode
import org.utbot.go.codegen.GoSimpleCodeGenerator
import java.io.File
import java.nio.file.Paths

object GoExecutor {

    object Constants {
        const val FILE_TO_EXECUTE_NAME = "ut_go_executor_tmp_file.go"

        // TODO: set up Go executor in general
        val GO_EXECUTOR_PATH = Paths.get("/home/gleb/go/go1.19rc1", "bin", "go").toString()
//        val GO_EXECUTOR_PATH = Paths.get(System.getenv("GOROOT"), "bin", "go").toString()
//        val GO_EXECUTOR_PATH = "go"

        // Note: codes must be correctly convertable into Regex (by .toRegex() method)
        const val NIL_VALUE_CODE = "%__go_exec_nil__%"
        const val DELIMITER_CODE = "%__go_exec_delim__%"
    }

    private val logger = KotlinLogging.logger {}

    fun invokeFunctionOrMethod(
        functionOrMethodNode: GoFunctionOrMethodNode,
        fuzzedParametersValues: List<FuzzedValue>
    ): GoUtExecutionResult {

        val fileToExecute = createGoFileToExecute(functionOrMethodNode.containingFileNode.containingPackagePath)
        try {
            val fileToExecuteCode = generateInvokeFunctionOrMethodGoCode(functionOrMethodNode, fuzzedParametersValues)
            fileToExecute.writeText(fileToExecuteCode)

            val command = listOf(
                Constants.GO_EXECUTOR_PATH,
                "run",
                fileToExecute.absolutePath
            )

            val executedProcess = runCatching {
                val process = ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start().also {
                        logger.debug { "GoExecutor process started with PID=${it.pid}" }
                    }
                process.waitFor()
                process
            }.getOrElse {
                throw RuntimeException(
                    "Execution of ${functionOrMethodNode.name} in child process failed with throwable: $it"
                )
            }
            val exitCode = executedProcess.exitValue()
            if (exitCode != 0) {
                throw RuntimeException(
                    "Execution of ${functionOrMethodNode.name} in child process failed with non-zero exit code = $exitCode"
                )
            }

            val functionOrMethodReturnOutput = executedProcess.inputStream.reader().readText()
            val functionOrMethodPanicOutput = executedProcess.errorStream.reader().readText()

            return mapToGoUtExecutionResult(
                functionOrMethodNode.returnCommonTypes,
                functionOrMethodReturnOutput,
                functionOrMethodPanicOutput
            )

        } finally {
//            fileToExecute.delete()
        }
    }

    private fun createGoFileToExecute(functionOrMethodContainingPackage: String): File {
        val file = Paths.get(functionOrMethodContainingPackage, Constants.FILE_TO_EXECUTE_NAME).toFile()
        file.createNewFile()
        return file
    }

    // TODO: use more convenient code generation
    private fun generateInvokeFunctionOrMethodGoCode(
        functionOrMethodNode: GoFunctionOrMethodNode,
        fuzzedParametersValues: List<FuzzedValue>
    ): String {
        val fileBuilder = GoSimpleCodeGenerator.GoFileCodeBuilder()

        fileBuilder.setPackage(functionOrMethodNode.containingFileNode.containingPackageName)
        fileBuilder.setImports(listOf("fmt", "io", "os"))

        val printOrExitFunctionDeclaration = """
            func __printToUtGoExecutorOrExit__(writer io.Writer, value any) {
            	_, err := fmt.Fprint(writer, value)
            	if err != nil {
            		os.Exit(1)
            	}
            }
        """.trimIndent()

        val printRvFunctionDeclaration = """
            func __printValueToUtGoExecutor__(writer io.Writer, value any, printPostfixSeparator bool) {
                const outputNil = "%__go_exec_nil__%"
                const outputDelimiter = "%__go_exec_delim__%"
            
                if value == nil {
                    __printToUtGoExecutorOrExit__(writer, outputNil)
            
                } else {
                    __printToUtGoExecutorOrExit__(writer, value)
                }
                if printPostfixSeparator {
                    __printToUtGoExecutorOrExit__(writer, outputDelimiter)
                }
            }
        """.trimIndent()

        val suppressOriginalStdOutputDeclaration = """
            stdout := os.Stdout
            stderr := os.Stderr
        
            null, _ := os.Open(os.DevNull)
            os.Stdout = null
            os.Stderr = null
        """.trimIndent()

        val panicCatcherFunction = """
            defer func() {
                panicMessage := recover()
                __printValueToUtGoExecutor__(stderr, panicked, true)
                __printValueToUtGoExecutor__(stderr, panicMessage, false)
            }()
        """.trimIndent()

        // rv = return values
        val rvCount = functionOrMethodNode.returnCommonTypes.size
        val rvVariablesNames = (1..rvCount).map { "rv$it" }

        // TODO: support methods
        val fuzzedFunctionCall = GoSimpleCodeGenerator.generateFuzzedFunctionCallSavedToVariables(
            rvVariablesNames,
            functionOrMethodNode,
            fuzzedParametersValues
        )

        val printRvVariablesCalls =
            rvVariablesNames.joinToString(separator = ", false)\n", postfix = ", true)") {
                "__printValueToUtGoExecutor__(stdout, $it"
            }

        val mainDeclaration = """
            func main() {
                ${suppressOriginalStdOutputDeclaration.indentize()}
            
                panicked := true
                ${panicCatcherFunction.indentize()}
                
                $fuzzedFunctionCall
                panicked = false
                
                ${printRvVariablesCalls.indentize()}
            }
        """.trimIndent()

        fileBuilder.addTopLevelElements(
            printOrExitFunctionDeclaration,
            printRvFunctionDeclaration,
            mainDeclaration
        )

        return fileBuilder.buildCodeString()
    }

    private fun String.indentize(indentsNumber: Int = 1): String {
        val tab = "\t".repeat(indentsNumber)
        return this.split("\n").joinToString(separator = "\n") { "$tab$it" }
    }

    private fun mapToGoUtExecutionResult(
        returnCommonTypes: List<GoCommonClassId>,
        returnRawValuesOutput: String,
        panicRawValuesOutput: String
    ): GoUtExecutionResult {

        val returnRawValues = returnRawValuesOutput.split(Constants.DELIMITER_CODE.toRegex())
        val panicRawValues = panicRawValuesOutput.split(Constants.DELIMITER_CODE.toRegex())

        if (panicRawValues.size != 2) {
            error("Panicked and panicMessage are expected in stderr.")
        }
        val (panicked, panicMessage) = panicRawValues
        if (panicked.toBoolean()) {
            return GoUtPanicFailure(if (panicMessage != Constants.NIL_VALUE_CODE) panicMessage else null)
        }

        if (returnRawValues.size != returnCommonTypes.size) {
            error("Function or method completed execution must have as many return values as return types.")
        }
        var executedWithNonNullErrorString = false
        val returnValues = returnRawValues.zip(returnCommonTypes).map { (returnRawValue, returnType) ->

            if (nonNullErrorReceived(returnRawValue, returnType)) {
                executedWithNonNullErrorString = true
            }

            // TODO: support errors fairly, i. e. as structs; for now consider them as strings
            val modelClassId = if (returnType.isErrorType) goStringClassId else returnType

            if (returnRawValue == Constants.NIL_VALUE_CODE) {
                GoUtNullModel(modelClassId)
            } else {
                // TODO: support complex types
                createGoUtPrimitiveModelFromRawValue(returnRawValue, modelClassId)
            }
        }

        return if (executedWithNonNullErrorString) {
            GoUtExecutionWithNonNullError(returnValues)
        } else {
            GoUtExecutionSuccess(returnValues)
        }
    }

    private fun nonNullErrorReceived(rawValue: String, classId: GoCommonClassId): Boolean {
        return classId.isErrorType && rawValue != Constants.NIL_VALUE_CODE
    }

    private fun createGoUtPrimitiveModelFromRawValue(rawValue: String, classId: GoCommonClassId): GoUtPrimitiveModel {
        val value = when (classId.correspondingKClass) {
            Byte::class -> rawValue.toByte()
            Short::class -> rawValue.toShort()
            Char::class -> rawValue.toCharArray().firstOrNull() ?: rawValue
            Int::class -> rawValue.toInt()
            Long::class -> rawValue.toLong()
            Float::class -> rawValue.toFloat()
            Double::class -> rawValue.toDouble()
            Boolean::class -> rawValue.toBoolean()
            else -> rawValue
        }
        return GoUtPrimitiveModel(value, classId)
    }
}