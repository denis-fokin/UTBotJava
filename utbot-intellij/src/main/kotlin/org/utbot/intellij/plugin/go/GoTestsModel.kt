package org.utbot.intellij.plugin.go

import com.goide.psi.GoFunctionOrMethodDeclaration
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.utbot.intellij.plugin.ui.utils.BaseTestsModel

class GoTestsModel(
    project: Project,
    srcModule: Module,
    testModule: Module,
    val functionsOrMethods: Set<GoFunctionOrMethodDeclaration>,
    val focusedFunctionOrMethod: GoFunctionOrMethodDeclaration?,
) : BaseTestsModel(
    project,
    srcModule,
    testModule
) {
    lateinit var selectedFunctionsOrMethods: Set<GoFunctionOrMethodDeclaration>
}