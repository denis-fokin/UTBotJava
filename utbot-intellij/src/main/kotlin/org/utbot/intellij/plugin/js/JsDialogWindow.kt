package org.utbot.intellij.plugin.js

import com.intellij.lang.javascript.refactoring.ui.JSMemberSelectionTable
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.components.Panel
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.intellij.plugin.ui.components.TestFolderComboWithBrowseButton
import java.awt.BorderLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel

class JsDialogWindow(val model: JsTestsModel): DialogWrapper(model.project) {

    private val items = model.fileMethods

    private val functionsTable = JSMemberSelectionTable(items, null, null).apply {
        items?.let {
            val height = this.rowHeight * (items.size.coerceAtMost(12) + 1)
            this.preferredScrollableViewportSize = JBUI.size(-1, height)
        }
    }

    private val testSourceFolderField = TestFolderComboWithBrowseButton(model)

    private val testFrameworks = ComboBox(DefaultComboBoxModel(TestFramework.allItems.toTypedArray()))

    private lateinit var panel: DialogPanel

    @Suppress("UNCHECKED_CAST")
    private val itemsToHelpTooltip = hashMapOf(
        (testFrameworks as ComboBox<CodeGenerationSettingItem>) to ContextHelpLabel.create(""),
    )

    init {
        title = "Generate tests with UtBot"
        setResizable(false)
        init()
    }

    @Suppress("UNCHECKED_CAST")
    override fun createCenterPanel(): JComponent {
        panel = panel {
            row("Test source root:") {
                component(testSourceFolderField)
            }
            row("Test framework:") {
                makePanelWithHelpTooltip(
                    testFrameworks as ComboBox<CodeGenerationSettingItem>,
                    itemsToHelpTooltip[testFrameworks]
                )
            }
            row("Generate test methods for:") {}
            row {
                scrollPane(functionsTable)
            }
        }
        checkBoxUpdate()
        return panel
    }

    private fun checkBoxUpdate() {
        if (items == null) return
        val focusedNames = model.focusedMethod?.map { it.name }
        val selectedMethods = items.filter {
            focusedNames?.contains(it.member.name) ?: false
        }
        if (selectedMethods.isEmpty()) {
            checkMembers(items)
        } else {
            checkMembers(selectedMethods)
        }
    }

    private fun checkMembers(members: Collection<JSMemberInfo>) = members.forEach { it.isChecked = true }

    private fun Row.makePanelWithHelpTooltip(
        mainComponent: JComponent,
        contextHelpLabel: ContextHelpLabel?
    ): CellBuilder<JPanel> =
        component(Panel().apply {
            add(mainComponent, BorderLayout.LINE_START)
            contextHelpLabel?.let { add(it, BorderLayout.LINE_END) }
        })
}