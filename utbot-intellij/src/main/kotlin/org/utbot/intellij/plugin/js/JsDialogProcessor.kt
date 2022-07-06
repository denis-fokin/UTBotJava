package org.utbot.intellij.plugin.js

import com.intellij.lang.javascript.psi.JSElement
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.idea.util.module
import org.utbot.intellij.plugin.ui.GenerateTestsDialogWindow
import org.utbot.intellij.plugin.ui.utils.testModule

class JsDialogProcessor {
    fun createDialogAndGenerateTests(
        project: Project,
        srcModule: Module,
        fileMethods: Set<JSMemberInfo>,
        focusedMethod: JSFunction?,
    ) {
        val dialogProcessor = JsDialogProcessor.createDialog(project, srcModule, fileMethods, focusedMethod)
        if(!dialogProcessor.showAndGet()) return

        JsDialogProcessor.createTests(project, dialogProcessor.model)
    }

    private fun createDialog(
        project: Project,
        srcModule: Module,
        fileMethods: Set<JSMemberInfo>,
        focusedMethod: JSFunction?,
    ) {
        val testModel = srcModule.testModule(project)

        return JsDialogWindow(
            JsTestsModel(
                project,
                srcModule,
                testModule,
                if (focusedMethod != null) setOf(focusedMethod) else null,
            )
        )
    }

    private fun createTests(project: Project, model: JsTestsModel) {
        //TODO
    }
}