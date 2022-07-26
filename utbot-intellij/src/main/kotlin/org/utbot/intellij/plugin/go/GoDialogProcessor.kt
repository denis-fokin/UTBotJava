package org.utbot.intellij.plugin.go

import com.goide.execution.target.GoLanguageRuntimeConfiguration
import com.goide.psi.GoFile
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.utbot.framework.plugin.api.GoCommonClassId
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

    private fun createTests(project: Project, model: GoTestsModel) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generate Go tests") {
            override fun run(indicator: ProgressIndicator) {

                // TODO: fix "Read access is allowed from event dispatch thread or inside read-action only"
                //  to enable operations with indicator
//                indicator.isIndeterminate = false
//                indicator.text = "Generate tests: read classes"

                val goFunctionsOrMethods = model.selectedFunctionsOrMethods

                val testCasesByFile = mutableMapOf<GoFile, MutableList<GoFuzzedFunctionOrMethodTestCase>>()

                for (goFunctionOrMethod in goFunctionsOrMethods) {
//                    indicator.text = "Generate test cases for ${goFunctionOrMethod.name}"
//                    indicator.fraction =
//                        indicator.fraction.coerceAtLeast(0.9 * processedFunctionsOrMethods / totalFunctionsOrMethods)

                    val testCases = generateTestCases(goFunctionOrMethod.toGoFunctionOrMethodNode())

                    val file = goFunctionOrMethod.containingFile
                    testCasesByFile.putIfAbsent(file, mutableListOf())
                    testCasesByFile[file]!!.addAll(testCases)
                }

//                indicator.fraction = indicator.fraction.coerceAtLeast(0.9)
//                indicator.text = "Generate code for tests"
                // Commented out to generate tests for collected executions even if action was canceled.
                // indicator.checkCanceled()

                invokeLater {
                    GoCodeGenerationController.generateTestsFilesAndCode(model, testCasesByFile.toMap())
                }
            }
        })
    }

    // TODO: fix "Read access is allowed from event dispatch thread or inside read-action only"
    private fun GoFunctionOrMethodDeclaration.toGoFunctionOrMethodNode(): GoFunctionOrMethodNode =
        GoFunctionOrMethodNode(
            this.name!!,
            run {
                // DEBUG
                val unusedDebug = 5
                val resultParameters = this.result?.parameters
                val resultType = this.result?.type
                println(
                    "resultParams: ${
                        resultParameters?.parameterDeclarationList?.mapNotNull {
                            GoCommonClassId(it.type!!.presentationText)
                        }
                    }"
                )
                println("resultType: ${resultType?.presentationText}")
                GoCommonClassId(this.resultType.presentationText)
            },
            this.signature!!.parameters.parameterDeclarationList.map { paramDecl ->
                GoFunctionOrMethodParameterNode(
                    paramDecl.namedUnwrappedElement!!.name!!,
                    GoCommonClassId(paramDecl.type!!.presentationText)
                )
            },
            GoBodyNode(this.block!!.text),
//            GoFileNode(File(containingFile.name).nameWithoutExtension, containingFile.canonicalPackageName!!)
            run {
                println(
                    GoFileNode(
                        File(containingFile.name).nameWithoutExtension,
                        containingFile.canonicalPackageName!!,
                        containingFile.containingDirectory!!.virtualFile.canonicalPath!!
                    )
                )
                GoFileNode(
                    File(containingFile.name).nameWithoutExtension,
                    containingFile.canonicalPackageName!!,
                    containingFile.containingDirectory!!.virtualFile.canonicalPath!!
                )
            }
        )
}