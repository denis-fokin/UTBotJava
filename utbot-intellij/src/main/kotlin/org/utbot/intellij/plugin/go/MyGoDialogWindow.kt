package org.utbot.intellij.plugin.go

import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import org.utbot.intellij.plugin.ui.components.TestFolderComboWithBrowseButton
import javax.swing.JComponent

class MyGoDialogWindow(val model: GoTestsModel) : DialogWrapper(model.project) {

    private val elements = model.functionsOrMethods.map { MyGoFunctionsOrMethodsChooserNode(it) }.toSet()

    private val functionsOrMethodsTable = MyGoFunctionsOrMethodsChooser(elements, model.project)

    private val testSourceFolderField = TestFolderComboWithBrowseButton(model)

    private lateinit var panel: DialogPanel

    init {
        title = "Generate tests with UtBot"
        setResizable(false)
        init()
    }

    override fun createCenterPanel(): JComponent {
        panel = panel {
            row("Test source root:") {
                component(testSourceFolderField)
            }
            row("Generate test methods for:") {}
            row {
                scrollPane(functionsOrMethodsTable.contentPane)
            }
        }
        updateFunctionsOrMethodsTable()
        return panel
    }

    override fun doOKAction() {
        model.selectedFunctionsOrMethods = functionsOrMethodsTable.selectedFunctionsOrMethods
        super.doOKAction()
    }

    private fun updateFunctionsOrMethodsTable() {
        if (elements.isEmpty()) isOKActionEnabled = false
        val focusedName = model.focusedFunctionOrMethod?.name
        val selectedFunctionsOrMethods = elements.filter {
            focusedName == it.functionOrMethod.name
        }.toSet()

        if (selectedFunctionsOrMethods.isEmpty()) {
            checkFunctionsOrMethods(elements)
        } else {
            checkFunctionsOrMethods(selectedFunctionsOrMethods)
        }
    }

    private fun checkFunctionsOrMethods(elementsToCheck: Set<MyGoFunctionsOrMethodsChooserNode>) {
        functionsOrMethodsTable.selectElements(elementsToCheck)
    }
}