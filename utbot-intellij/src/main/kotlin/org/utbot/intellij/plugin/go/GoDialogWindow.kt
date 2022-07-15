package org.utbot.intellij.plugin.go

import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.refactor.ui.GoDeclarationInfo
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.FakeVirtualFile
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.utbot.intellij.plugin.ui.components.TestFolderComboWithBrowseButton
import javax.swing.JComponent

@Suppress("DuplicatedCode")
class GoDialogWindow(val model: GoTestsModel) : DialogWrapper(model.project) {

    private val allInfos = model.functionsOrMethods.toInfos()

    private val functionsOrMethodsTable = GoFunctionsOrMethodsSelectionTable(allInfos).apply {
        // copied from GenerateTestsDialogWindow
        val height = this.rowHeight * (allInfos.size.coerceAtMost(12) + 1)
        this.preferredScrollableViewportSize = JBUI.size(-1, height)
    }

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
                scrollPane(functionsOrMethodsTable)
            }
        }
        updateFunctionsOrMethodsTable()
        return panel
    }

    override fun doOKAction() {
        model.selectedFunctionsOrMethods = functionsOrMethodsTable.selectedMemberInfos.fromInfos()
        model.srcFiles = model.selectedFunctionsOrMethods
            .map { it.containingFile }
            .toSet()

        super.doOKAction()
    }

    private fun updateFunctionsOrMethodsTable() {
        val focusedName = model.focusedFunctionOrMethod?.name
        val selectedInfos = allInfos.filter {
            focusedName == it.declaration.name
        }
        if (selectedInfos.isEmpty()) {
            checkInfos(allInfos)
        } else {
            checkInfos(selectedInfos)
        }
        functionsOrMethodsTable.setMemberInfos(allInfos)

        if (functionsOrMethodsTable.selectedMemberInfos.isEmpty()) {
            isOKActionEnabled = false
        }
    }

    private fun checkInfos(infos: Collection<GoDeclarationInfo>) {
        infos.forEach { it.isChecked = true }
    }

    private fun Collection<GoFunctionOrMethodDeclaration>.toInfos(): Set<GoDeclarationInfo> =
        this.map { GoDeclarationInfo(it) }.toSet()

    private fun Collection<GoDeclarationInfo>.fromInfos(): Set<GoFunctionOrMethodDeclaration> =
        this.map { it.declaration as GoFunctionOrMethodDeclaration }.toSet()

    /* further code is copied from GenerateTestsDialogWindow with very little change (createPackagesByFiles) */

    override fun doValidate(): ValidationInfo? {
        val testRoot = getTestRoot()
            ?: return ValidationInfo("Test source root is not configured", testSourceFolderField.childComponent)

        if (findReadOnlyContentEntry(testRoot) == null) {
            return ValidationInfo(
                "Test source root is located out of content entry",
                testSourceFolderField.childComponent
            )
        }

        functionsOrMethodsTable.tableHeader?.background = UIUtil.getTableBackground()
        functionsOrMethodsTable.background = UIUtil.getTableBackground()
        if (functionsOrMethodsTable.selectedMemberInfos.isEmpty()) {
            functionsOrMethodsTable.tableHeader?.background = JBUI.CurrentTheme.Validator.errorBackgroundColor()
            functionsOrMethodsTable.background = JBUI.CurrentTheme.Validator.errorBackgroundColor()
            return ValidationInfo(
                "Tick any methods to generate tests for", functionsOrMethodsTable
            )
        }
        return null
    }

    private fun getTestRoot(): VirtualFile? {
        model.testSourceRoot?.let {
            if (it.isDirectory || it is FakeVirtualFile) return it
        }
        return null
    }

    private fun findReadOnlyContentEntry(testSourceRoot: VirtualFile?): ContentEntry? {
        if (testSourceRoot == null) return null
        if (testSourceRoot is FakeVirtualFile) {
            return findReadOnlyContentEntry(testSourceRoot.parent)
        }
        return ModuleRootManager.getInstance(model.testModule).contentEntries
            .filterNot { it.file == null }
            .firstOrNull { VfsUtil.isAncestor(it.file!!, testSourceRoot, false) }
    }
}