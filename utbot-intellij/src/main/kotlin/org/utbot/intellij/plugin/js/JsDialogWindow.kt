package org.utbot.intellij.plugin.js

import com.intellij.lang.javascript.refactoring.ui.JSMemberSelectionTable
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import com.oracle.js.parser.ErrorManager
import com.oracle.js.parser.Parser
import com.oracle.js.parser.ScriptEnvironment
import com.oracle.js.parser.Source
import com.oracle.js.parser.ir.FunctionNode
import com.oracle.js.parser.ir.VarNode
import org.graalvm.polyglot.Context
import org.utbot.framework.plugin.api.JsPrimitiveModel
import org.utbot.fuzzer.FuzzedValue
import org.utbot.intellij.plugin.js.fuzzer.jsFuzzing
import org.utbot.intellij.plugin.ui.components.TestFolderComboWithBrowseButton
import javax.swing.JComponent


class JsDialogWindow(val model: JsTestsModel) : DialogWrapper(model.project) {

    private val items = model.fileMethods

    private val functionsTable = JSMemberSelectionTable(items, null, null).apply {
        val height = this.rowHeight * (items.size.coerceAtMost(12) + 1)
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
                scrollPane(functionsTable)
            }
        }
        updateMembersTable()
        return panel
    }

    override fun doOKAction() {
        val selected = functionsTable.selectedMemberInfos
        selected.forEach {
            val funcNode = getFunctionNode(it)
            val res = jsFuzzing(method = funcNode).toList()
            runJs(res, funcNode, it.member.text)
        }
        super.doOKAction()
    }

    private fun runJs(fuzzedValues: List<List<FuzzedValue>>, method: FunctionNode, funcString: String) {
        val context = Context.newBuilder("js").build()
        fuzzedValues.forEach { values ->
            val str = makeStringForRunJs(values, method, funcString)
            val res = context.eval("js", str)
            val k = 1
        }
    }

    private fun makeStringForRunJs(fuzzedValue: List<FuzzedValue>, method: FunctionNode, funcString: String): String {
        val callString = makeCallFunctionString(fuzzedValue, method)
        return """function $funcString
                  $callString""".trimIndent()
    }

    private fun makeCallFunctionString(fuzzedValue: List<FuzzedValue>, method: FunctionNode): String {
        var callString = "${method.name}("
        fuzzedValue.forEach { value ->
            when ((value.model as JsPrimitiveModel).value) {
                is String -> callString += "\"${(value.model as JsPrimitiveModel).value}\","
                else -> callString += "${(value.model as JsPrimitiveModel).value},"
            }
        }
        callString = callString.dropLast(1)
        callString += ')'
        return callString
    }

    private fun getFunctionNode(focusedMethod: JSMemberInfo): FunctionNode {
        val funFixString = "function " + focusedMethod.member.text
        Thread.currentThread().contextClassLoader = Context::class.java.classLoader
        val parser = Parser(
            ScriptEnvironment.builder().build(),
            Source.sourceFor("test", funFixString),
            ErrorManager.ThrowErrorManager()
        )
        val functionNode = parser.parse()
        val block = functionNode.body
        return (block.statements[0] as VarNode).init as FunctionNode
    }

    private fun updateMembersTable() {
        if (items.isEmpty()) isOKActionEnabled = false
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
}