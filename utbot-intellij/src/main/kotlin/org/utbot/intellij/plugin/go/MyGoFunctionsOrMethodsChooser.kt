package org.utbot.intellij.plugin.go

import com.goide.psi.GoFunctionOrMethodDeclaration
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

class MyGoFunctionsOrMethodsChooser(
    private val elements: Set<MyGoFunctionsOrMethodsChooserNode>,
    project: Project
) : DialogWrapper(project, true) {

    val selectedFunctionsOrMethods: Set<GoFunctionOrMethodDeclaration>
        get() = elements
            .filter { it.isSelected }
            .map { it.functionOrMethod }
            .toSet()

    override fun createCenterPanel(): JComponent? {
        TODO("Not yet implemented")
    }

    fun selectElements(elementsToSelect: Set<MyGoFunctionsOrMethodsChooserNode>) {
        // TODO
    }

}