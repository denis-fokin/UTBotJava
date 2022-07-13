package org.utbot.intellij.plugin.go

import com.goide.intentions.generate.constructor.GoMemberChooser
import com.goide.intentions.generate.constructor.GoMemberChooserNode
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import org.utbot.intellij.plugin.ui.components.TestFolderComboWithBrowseButton
import javax.swing.JComponent

class GoDialogWindow(val model: GoTestsModel) : DialogWrapper(model.project) {

    private val items = model.functionsOrMethods.map { GoMemberChooserNode(it) }.toSet()

    private val methodsTable = GoMemberChooser(items.toTypedArray(), model.project, null)

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
                scrollPane(methodsTable.contentPane)
            }
        }
        updateMembersTable()
        return panel
    }

    private fun updateMembersTable() {
        if (items.isEmpty()) isOKActionEnabled = false
        val focusedName = model.focusedFunctionOrMethod?.name
        val selectedMethods = items.filter {
            focusedName == it.text
        }
        if (selectedMethods.isEmpty()) {
            checkMembers(items)
        } else {
            checkMembers(selectedMethods)
        }
    }

    private fun checkMembers(members: Collection<GoMemberChooserNode>) {
        methodsTable.selectElements(members.toTypedArray())
    }
}