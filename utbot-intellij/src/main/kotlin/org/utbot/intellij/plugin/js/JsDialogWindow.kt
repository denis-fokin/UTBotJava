package org.utbot.intellij.plugin.js

import com.intellij.lang.javascript.refactoring.ui.JSMemberSelectionTable
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import javax.swing.JComponent


class JsDialogWindow(val model: JsTestsModel): DialogWrapper(model.project) {

    private val items = model.fileMethods

    private val functionsTable = JSMemberSelectionTable(items, null, "").apply {
        items?.let {
            val height = this.rowHeight * (items.size.coerceAtMost(12) + 1)
            this.preferredScrollableViewportSize = JBUI.size(-1, height)
        }
    }

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
        return panel
    }

    private fun initDefaultValues() {
    }

    private fun setListeners() {
    }

    private fun checkMembers(members: Collection<JSMemberInfo>) = members.forEach { it.isChecked = true }
}