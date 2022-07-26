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
import java.io.InputStreamReader
import java.nio.file.Paths

object GoExecutor {

    object Constants {
        const val FILE_TO_EXECUTE_NAME = "ut_go_executor_tmp_file_test.go"

        // TODO: set up Go executor in general
        val GO_EXECUTOR_PATH = Paths.get("/home/gleb/go/go1.19rc1", "bin", "go").toString()
//        val GO_EXECUTOR_PATH = Paths.get(System.getenv("GOROOT"), "bin", "go").toString()
//        val GO_EXECUTOR_PATH = "go"

        // Note: codes must be correctly convertable into Regex (by .toRegex() method)
        const val NIL_VALUE_CODE = "%__go_exec_nil__%"
        const val DELIMITER_CODE = "%__go_exec_delim__%"

        const val EXECUTION_RESULT_OUTPUT_FILE_NAME = "ut_go_executor_out_communication.temp"
        const val EXECUTION_RESULT_ERROR_FILE_NAME = "ut_go_executor_err_communication.temp"
    }

    private val logger = KotlinLogging.logger {}

    fun invokeFunctionOrMethod(
        functionOrMethodNode: GoFunctionOrMethodNode,
        fuzzedParametersValues: List<FuzzedValue>
    ): GoUtExecutionResult {

        val fileToExecute = createGoFileToExecute(functionOrMethodNode.containingFileNode.containingPackagePath)

        val packageDirectoryFile = File(functionOrMethodNode.containingFileNode.containingPackagePath).absoluteFile
        val executionOutputFile = packageDirectoryFile.resolve(Constants.EXECUTION_RESULT_OUTPUT_FILE_NAME)
        val executionErrorFile = packageDirectoryFile.resolve(Constants.EXECUTION_RESULT_ERROR_FILE_NAME)

        try {
            val fileToExecuteCode = generateInvokeFunctionOrMethodGoCode(functionOrMethodNode, fuzzedParametersValues)
            fileToExecute.writeText(fileToExecuteCode)

            // TODO: get correct module name and run tests for it; for now: locally from package directory
            val command = listOf(
                Constants.GO_EXECUTOR_PATH,
                "test",
                "-run",
                createTestFunctionName(functionOrMethodNode.name)
            )


            val executedProcess = runCatching {
                val process = ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .directory(packageDirectoryFile)
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
                println("ERRORS")
                println(InputStreamReader(executedProcess.errorStream).readText())
                println("\nSOUT")
                println(InputStreamReader(executedProcess.inputStream).readText())

                throw RuntimeException(
                    "Execution of ${functionOrMethodNode.name} in child process failed with non-zero exit code = $exitCode"
                )
            }

            val functionOrMethodReturnOutput = executionOutputFile.readText()
            val functionOrMethodPanicOutput = executionErrorFile.readText()

            return mapToGoUtExecutionResult(
                functionOrMethodNode.returnCommonTypes,
                functionOrMethodReturnOutput,
                functionOrMethodPanicOutput
            )

        } finally {
            fileToExecute.delete()
            executionErrorFile.delete()
            executionOutputFile.delete()
        }
    }

    private fun createGoFileToExecute(functionOrMethodContainingPackage: String): File {
        val file = Paths.get(functionOrMethodContainingPackage, Constants.FILE_TO_EXECUTE_NAME).toFile()
        file.createNewFile()
        return file
    }

    private fun createTestFunctionName(functionOrMethodName: String): String {
        return "Test${functionOrMethodName.capitalize()}ByUtGoExecutor"
    }

    // TODO: use more convenient code generation
    private fun generateInvokeFunctionOrMethodGoCode(
        functionOrMethodNode: GoFunctionOrMethodNode,
        fuzzedParametersValues: List<FuzzedValue>
    ): String {
        val fileBuilder = GoSimpleCodeGenerator.GoFileCodeBuilder()

        fileBuilder.setPackage(functionOrMethodNode.containingFileNode.containingPackageName)
        fileBuilder.setImports(listOf("bufio", "fmt", "io", "os", "testing"))

        val checkErrorFunctionDeclaration = """
            func __checkErrorAndExitToUtGoExecutor__(err error) {
            	if err != nil {
            		os.Exit(1)
            	}
            }
        """.trimIndent()

        val printOrExitFunctionDeclaration = """
            func __printToUtGoExecutorOrExit__(writer io.Writer, value any) {
                _, err := fmt.Fprint(writer, value)
                __checkErrorAndExitToUtGoExecutor__(err)
            }
        """.trimIndent()

        val printValueFunctionDeclaration = """
            func __printValueToUtGoExecutor__(writer io.Writer, value any, printPostfixSeparator bool) {
                const outputNil = "${Constants.NIL_VALUE_CODE}"
                const outputDelimiter = "${Constants.DELIMITER_CODE}"
            
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

        val createWriterFunctionDeclaration = """
            func __createWriterToUtGoExecutorFile__(fileName string) (*os.File, *bufio.Writer) {
            	file, err := os.Create(fileName)
            	__checkErrorAndExitToUtGoExecutor__(err)
            	return file, bufio.NewWriter(file)
            }
        """.trimIndent()

        val closeWriterFunctionDeclaration = """
            func __closeWriterToUtGoExecutorFile__(file *os.File, writer *bufio.Writer) {
                flushErr := writer.Flush()
                __checkErrorAndExitToUtGoExecutor__(flushErr)
            
                closeErr := file.Close()
                __checkErrorAndExitToUtGoExecutor__(closeErr)
            }
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
            rvVariablesNames.joinToString(separator = ", true)\n\t", postfix = ", false)") {
                "__printValueToUtGoExecutor__(outWriter, $it"
            }

        val executionTestDeclaration = """
            func ${createTestFunctionName(functionOrMethodNode.name)}(t *testing.T) {
                outFile, outWriter := __createWriterToUtGoExecutorFile__("${Constants.EXECUTION_RESULT_OUTPUT_FILE_NAME}")
                errFile, errWriter := __createWriterToUtGoExecutorFile__("${Constants.EXECUTION_RESULT_ERROR_FILE_NAME}")
        
                panicked := true
                defer func() {
                    panicMessage := recover()
                    __printValueToUtGoExecutor__(errWriter, panicked, true)
                    __printValueToUtGoExecutor__(errWriter, panicMessage, false)
            
                    __closeWriterToUtGoExecutorFile__(outFile, outWriter)
                    __closeWriterToUtGoExecutorFile__(errFile, errWriter)
                }()
                
                $fuzzedFunctionCall
                panicked = false
                
                $printRvVariablesCalls
            }
        """.trimIndent()

        fileBuilder.addTopLevelElements(
            checkErrorFunctionDeclaration,
            printOrExitFunctionDeclaration,
            printValueFunctionDeclaration,
            createWriterFunctionDeclaration,
            closeWriterFunctionDeclaration,
            executionTestDeclaration
        )

        return fileBuilder.buildCodeString()
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