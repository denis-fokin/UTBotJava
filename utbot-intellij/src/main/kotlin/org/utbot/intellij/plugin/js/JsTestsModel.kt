package org.utbot.intellij.plugin.js

import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.testIntegration.TestFramework
import org.utbot.intellij.plugin.ui.utils.BaseTestsModel

class JsTestsModel(
    project: Project,
    srcModule: Module,
    testModule: Module,
    val fileMethods: Set<JSMemberInfo>,
    val focusedMethod: Set<JSFunction>?,
) : BaseTestsModel(
    project,
    srcModule,
    testModule
) {
    lateinit var testFramework: TestFramework
}
