package org.utbot.intellij.plugin.js

import com.intellij.lang.javascript.refactoring.ui.JSMemberSelectionTable
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.lang.jvm.actions.updateMethodParametersRequest
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.refactoring.ui.MemberSelectionTable
import com.intellij.refactoring.util.classMembers.MemberInfo
import com.intellij.testIntegration.TestIntegrationUtils
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import javax.swing.JComponent


class JsDialogWindow(val model: JsTestsModel): DialogWrapper(model.project) {

    private val functionsTable = JSMemberSelectionTable(emptyList(), null, "")

    private val testSourceFolderField = JsTestFolderComboWithBrowseButton(model)

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
                scrollPane(functionsTable)
            }
        }
        initDefaultValues()
        updateFunctionsTable()
        return panel
    }

    private fun initDefaultValues() {
    }
    private fun setListeners() {
    }

    private fun updateFunctionsTable() {
        val items = model.fileMethods
        items?.let{
            updateMethodsTable(items)
            val height = functionsTable.rowHeight * (items.size.coerceAtMost(12) + 1)
            functionsTable.preferredScrollableViewportSize = JBUI.size(-1, height)
        }
    }

    private fun updateMethodsTable(allMethods: Collection<JSMemberInfo>) {
        val focusedNames = model.focusedMethod?.map { it.name }
        val selectedMethods = allMethods.filter {
            focusedNames?.contains(it.member.name) ?: false
        }
        if (selectedMethods.isEmpty()) {
            checkMembers(allMethods)
        } else {
            checkMembers(selectedMethods)
        }
        functionsTable.setMemberInfos(allMethods)
    }

    private fun checkMembers(members: Collection<JSMemberInfo>) = members.forEach { it.isChecked = true }
}