package org.utbot.intellij.plugin.js

import com.intellij.lang.javascript.refactoring.ui.JSMemberSelectionTable
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.Panel
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import org.utbot.framework.codegen.Mocha
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.intellij.plugin.ui.components.TestFolderComboWithBrowseButton
import java.awt.BorderLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import kotlin.concurrent.thread


class JsDialogWindow(val model: JsTestsModel) : DialogWrapper(model.project) {

    private val items = model.fileMethods

    private val functionsTable = JSMemberSelectionTable(items, null, null).apply {
        val height = this.rowHeight * (items.size.coerceAtMost(12) + 1)
        this.preferredScrollableViewportSize = JBUI.size(-1, height)
    }

    private val testSourceFolderField = TestFolderComboWithBrowseButton(model)
    private val testFrameworks: ComboBox<TestFramework> = ComboBox(DefaultComboBoxModel(arrayOf(Mocha)))

    private var initTestFrameworkPresenceThread: Thread

    private lateinit var panel: DialogPanel

    init {
        title = "Generate tests with UtBot"
        initTestFrameworkPresenceThread = thread(start = true) {
            TestFramework.allJsItems.forEach {
                it.isInstalled = findFrameworkLibrary(it.displayName.toLowerCase())
            }
        }
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
                component(
                    Panel().apply {
                        add(testFrameworks as ComboBox<CodeGenerationSettingItem>, BorderLayout.LINE_START)
                    }
                )
            }
            row("Generate test methods for:") {}
            row {
                scrollPane(functionsTable)
            }
        }
        updateMembersTable()
        return panel
    }

    override fun doOKAction() {
        val selected = functionsTable.selectedMemberInfos.toSet()
        model.selectedMethods = if (selected.any()) selected else null
        model.testFramework = testFrameworks.item
        configureTestFrameworkIfRequired()
        super.doOKAction()
    }

    private fun updateMembersTable() {
        if (items.isEmpty()) isOKActionEnabled = false
        val focusedNames = model.selectedMethods?.map { it.member.name }
        val selectedMethods = items.filter {
            focusedNames?.contains(it.member.name) ?: false
        }
        if (selectedMethods.isEmpty()) {
            checkMembers(items)
        } else {
            checkMembers(selectedMethods)
        }
    }

    private fun configureTestFramework() {
        val selectedTestFramework = testFrameworks.item
        selectedTestFramework.isInstalled = true
        val packageInstallBuilder = ProcessBuilder("cmd.exe", "/c", "npm install -l ${selectedTestFramework.displayName.toLowerCase()}")
        val packageInstallProcess = packageInstallBuilder.start()
        packageInstallProcess.waitFor()
    }

    private fun configureTestFrameworkIfRequired() {
        initTestFrameworkPresenceThread.join()
        val frameworkNotInstalled = !testFrameworks.item.isInstalled
        if (frameworkNotInstalled && createTestFrameworkNotificationDialog() == Messages.YES) {
            (object : Task.Backgroundable(model.project, "Install test framework package") {
                override fun run(indicator: ProgressIndicator) {
                    indicator.text = "Installing ${testFrameworks.item.displayName} npm package"
                    configureTestFramework()
                }

            }).queue()
        }
    }

    private fun createTestFrameworkNotificationDialog() = Messages.showYesNoDialog(
        """Selected test framework ${testFrameworks.item.displayName} is not installed into current module. 
            |Would you like to install it now?""".trimMargin(),
        title,
        "Yes",
        "No",
        Messages.getQuestionIcon(),
    )

    private fun findFrameworkLibrary(npmPackageName: String): Boolean {
        val nodeBuilder = ProcessBuilder("cmd.exe", "/c", "npm list")
        val nodeProcess = nodeBuilder.start()
        val bufferedReader = nodeProcess.inputStream.bufferedReader()
        val checkForPackageText = bufferedReader.readText()
        bufferedReader.close()
        if (checkForPackageText == "") {
            Messages.showErrorDialog(
                model.project,
                "Node.js is not installed",
                title,
            )
            return false
        }
        return checkForPackageText.contains(npmPackageName)
    }

    private fun checkMembers(members: Collection<JSMemberInfo>) = members.forEach { it.isChecked = true }
}