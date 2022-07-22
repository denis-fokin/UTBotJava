package org.utbot.intellij.plugin.generator

import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.oracle.js.parser.ErrorManager
import com.oracle.js.parser.Parser
import com.oracle.js.parser.ScriptEnvironment
import com.oracle.js.parser.Source
import com.oracle.js.parser.ir.FunctionNode
import com.oracle.js.parser.ir.VarNode
import fuzzer.JsAstVisitor
import fuzzer.JsFuzzer.jsFuzzing
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.jsUndefinedClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.names.ModelBasedNameSuggester
import org.utbot.intellij.plugin.ui.JsDialogWindow
import org.utbot.intellij.plugin.models.JsTestsModel
import org.utbot.intellij.plugin.ui.utils.testModule
import kotlin.random.Random

object JsDialogProcessor {

    fun createDialogAndGenerateTests(
        project: Project,
        srcModule: Module,
        fileMethods: Set<JSMemberInfo>,
        focusedMethod: JSMemberInfo?,
    ) {
        val dialogProcessor = createDialog(project, srcModule, fileMethods, focusedMethod)
        if (!dialogProcessor.showAndGet()) return

        createTests(dialogProcessor.model)
    }

    private fun createDialog(
        project: Project,
        srcModule: Module,
        fileMethods: Set<JSMemberInfo>,
        focusedMethod: JSMemberInfo?,
    ): JsDialogWindow {
        val testModel = srcModule.testModule(project)

        return JsDialogWindow(
            JsTestsModel(
                project,
                srcModule,
                testModel,
                fileMethods,
                if (focusedMethod != null) setOf(focusedMethod) else null,
            )
        )
    }

    private fun createTests(model: JsTestsModel) {
        model.selectedMethods?.forEach { jsMemberInfo ->
            val funcNode = getFunctionNode(jsMemberInfo)
            //TODO: think of JsClassId("debug") and jsUndefinedClassId usages
            val execId = MethodId(
                JsClassId("debug"),
                funcNode.name.toString(),
                jsUndefinedClassId,
                funcNode.parameters.toList().map { jsUndefinedClassId }
            )
            funcNode.body.accept(JsAstVisitor)
            val methodUnderTestDescription = FuzzedMethodDescription(execId, JsAstVisitor.fuzzedConcreteValues).apply {
                compilableName = funcNode.name.toString()
                val names = funcNode.parameters.map { it.name.toString() }
                parameterNameMap = { index -> names.getOrNull(index) }
            }
            val fuzzedValues =
                jsFuzzing(method = funcNode, methodUnderTestDescription = methodUnderTestDescription).toList()
            //For dev purposes only random set of fuzzed values is picked. TODO: patch this later
            val randomParams = getRandomNumFuzzedValues(fuzzedValues)
            val testsForGenerator = mutableListOf<Sequence<*>>()
            randomParams.forEach { param ->
                val returnValue = runJs(param, funcNode, jsMemberInfo.member.text)
//                val testCodeGen = JsTestCodeGenerator.generateTestCode(funcNode, param, JsPrimitiveModel(returnValue))
                // For dev purposes only 10 random sets of fuzzed values is picked. TODO: patch this later
                // Hack: Should create one file with all fun to compile? TODO: think
                val params = getRandomNumFuzzedValues(fuzzedValues)
                val testsForGenerator = mutableListOf<Sequence<*>>()
                params.forEach { param ->
                    val returnValue = runJs(param, funcNode, jsMemberInfo.member.text)
                    testsForGenerator.add(
                        ModelBasedNameSuggester().suggest(
                            methodUnderTestDescription,
                            param,
                            UtExecutionSuccess(JsPrimitiveModel(returnValue))
                        )
                    )
                }
            }
        }
    }

    private fun getRandomNumFuzzedValues(fuzzedValues: List<List<FuzzedValue>>): List<List<FuzzedValue>> {
        val newFuzzedValues = mutableListOf<List<FuzzedValue>>()
        for (i in 0..10) {
            newFuzzedValues.add(fuzzedValues[Random.nextInt(fuzzedValues.size)])
        }
        return newFuzzedValues
    }

    private fun runJs(fuzzedValues: List<FuzzedValue>, method: FunctionNode, funcString: String): Value {
        val context = Context.newBuilder("js").build()
        val str = makeStringForRunJs(fuzzedValues, method, funcString)
        return context.eval("js", str)
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

}