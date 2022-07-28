package org.utbot.intellij.plugin.go

import com.goide.psi.GoFile
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.psi.GoType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.debugger.readAction
import org.utbot.framework.plugin.api.GoTypeId
import org.utbot.go.*
import org.utbot.go.fuzzer.generateTestCases
import org.utbot.intellij.plugin.go.codegen.GoCodeGenerationController
import org.utbot.intellij.plugin.ui.utils.testModule
import java.io.File

object GoDialogProcessor {

    fun createDialogAndGenerateTests(
        project: Project,
        srcModule: Module,
        functionsOrMethod: Set<GoFunctionOrMethodDeclaration>,
        focusedFunctionOrMethod: GoFunctionOrMethodDeclaration?,
    ) {
        val dialogProcessor = createDialog(project, srcModule, functionsOrMethod, focusedFunctionOrMethod)
        if (!dialogProcessor.showAndGet()) return

        createTests(project, dialogProcessor.model)
    }

    private fun createDialog(
        project: Project,
        srcModule: Module,
        functionsOrMethod: Set<GoFunctionOrMethodDeclaration>,
        focusedFunctionOrMethod: GoFunctionOrMethodDeclaration?,
    ): GoDialogWindow {
        val testModel = srcModule.testModule(project)

        return GoDialogWindow(
            GoTestsModel(
                project,
                srcModule,
                testModel,
                functionsOrMethod,
                focusedFunctionOrMethod,
            )
        )
    }

    private object ProgressIndicatorConstants {
        const val READ_FILES_FRACTION = 0.05
        const val GENERATE_CODE_FRACTION = 0.1
        const val GENERATE_TEST_CASES_FRACTION = 1.0 - READ_FILES_FRACTION - GENERATE_CODE_FRACTION
    }

    private fun createTests(project: Project, model: GoTestsModel) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generate Go tests") {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.text = "Generate tests: read files"
                indicator.fraction = 0.0

                val goFunctionsOrMethods = model.selectedFunctionsOrMethods
                indicator.fraction = indicator.fraction.coerceAtLeast(ProgressIndicatorConstants.READ_FILES_FRACTION)

                val testCasesByFile = mutableMapOf<GoFile, MutableList<GoFuzzedFunctionOrMethodTestCase>>()
                goFunctionsOrMethods.forEachIndexed { processedFunctionsOrMethods, goFunctionOrMethod ->
                    indicator.text = "Generate test cases for ${goFunctionOrMethod.name}"
                    indicator.fraction = indicator.fraction.coerceAtLeast(
                        ProgressIndicatorConstants.READ_FILES_FRACTION +
                            ProgressIndicatorConstants.GENERATE_TEST_CASES_FRACTION *
                            processedFunctionsOrMethods / goFunctionsOrMethods.size
                    )
                    readAction { // to read PSI-tree or else "Read access" exception
                        val testCases = generateTestCases(goFunctionOrMethod.toGoFunctionOrMethodNode())
                        val file = goFunctionOrMethod.containingFile
                        testCasesByFile.putIfAbsent(file, mutableListOf())
                        testCasesByFile[file]!!.addAll(testCases)
                    }
                }

                indicator.fraction =
                    indicator.fraction.coerceAtLeast(
                        ProgressIndicatorConstants.READ_FILES_FRACTION +
                            ProgressIndicatorConstants.GENERATE_TEST_CASES_FRACTION
                    )
                indicator.text = "Generate code for tests"
                // Commented out to generate tests for collected executions even if action was canceled.
                // indicator.checkCanceled()

                invokeLater {
                    GoCodeGenerationController.generateTestsFilesAndCode(model, testCasesByFile.toMap())
                }
            }
        })
    }

    private fun GoFunctionOrMethodDeclaration.toGoFunctionOrMethodNode(): GoFunctionOrMethodNode =
        GoFunctionOrMethodNode(
            this.name!!,
            run {
                val result = this.result ?: return@run emptyList<GoTypeId>()

                // Exactly one of result.type and result.parameters is non-null.
                val returnType = result.type
                if (returnType != null) {
                    return@run listOf(GoTypeId(returnType.presentationText, isErrorType = returnType.isErrorType()))
                }
                val returnTypes = result.parameters!!.parameterDeclarationList.map {
                    val type = it.type!!
                    GoTypeId(type.presentationText, isErrorType = type.isErrorType())
                }
                return@run returnTypes
            },
            this.signature!!.parameters.parameterDeclarationList.map { paramDecl ->
                GoFunctionOrMethodParameterNode(
                    paramDecl.namedUnwrappedElement!!.name!!,
                    GoTypeId(paramDecl.type!!.presentationText)
                )
            },
            GoBodyNode(this.block!!.text),
            GoFileNode(
                File(containingFile.name).nameWithoutExtension,
                containingFile.canonicalPackageName!!,
                containingFile.containingDirectory!!.virtualFile.canonicalPath!!
            )
        )

    // TODO: check if type implements error via plugin
    private fun GoType.isErrorType(): Boolean {
        return this.presentationText == "error"
    }
}