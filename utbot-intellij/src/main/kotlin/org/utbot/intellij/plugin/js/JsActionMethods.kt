package org.utbot.intellij.plugin.js

import com.intellij.lang.Language
import com.intellij.lang.ecmascript6.psi.ES6Class
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.refactoring.util.JSMemberInfo
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.util.projectStructure.module

object JsActionMethods {

    private const val jsId = "ECMAScript 6"
    val jsLanguage : Language = Language.findLanguageByID(jsId) ?: error("JavaScript language wasn't found")

    private data class PsiTargets(
        val methods: Set<JSMemberInfo>,
        val focusedMethod: JSFunction?,
        val module: Module,
    )

    fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val (methods, focusedMethod, module) = getPsiTargets(e) ?: return
        JsDialogProcessor.createDialogAndGenerateTests(
            project,
            module,
            methods,
            focusedMethod
        )
    }

    fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getPsiTargets(e) != null
    }

    private fun getPsiTargets(e: AnActionEvent): PsiTargets? {
        e.project ?: return null
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        val file = e.getData(CommonDataKeys.PSI_FILE) as? JSFile ?: return null
        val element = findPsiElement(file, editor) ?: return null
        val module = element.module ?: return null
        val focusedMethod = getContainingMethod(element)
        containingClass(element)?.let {
            val methods = it.functions ?: return null
            return PsiTargets(
                generateMemberInfo(e.project!!, methods.toList()),
                focusedMethod,
                module,
            )
        }
        return PsiTargets(
            generateMemberInfo(e.project!!, file.statements.filterIsInstance<JSFunction>()),
            focusedMethod,
            module,
        )
    }

    private fun getContainingMethod(element: PsiElement): JSFunction? {
        if (element is JSFunction)
            return element

        val parent = element.parent ?: return null
        return getContainingMethod(parent)
    }

    private fun findPsiElement(file: PsiFile, editor: Editor): PsiElement? {
        val offset = editor.caretModel.offset
        var element = file.findElementAt(offset)
        if (element == null && offset == file.textLength) {
            element = file.findElementAt(offset - 1)
        }
        return element
    }

    private fun containingClass(element: PsiElement) =
        PsiTreeUtil.getParentOfType(element, ES6Class::class.java, false)

    private fun buildClassStringFromMethods(methods: List<JSFunction>): String {
        var strBuilder = "\n"
        val filteredMethods = methods.filterNot { method -> method.name == "constructor" }
        filteredMethods.forEach {
            strBuilder += buildStringForMethod(it)
        }
        return "class askfajsghalwig {$strBuilder}"
    }

    private fun buildStringForMethod(method: JSFunction): String {
        val str = method.parameterVariables.map { it.name ?: error("No Js method name!") }
            .fold("") {acc, s -> "$acc, $s" }
        return "${method.name} (${str.drop(1)}) {}\n"
    }
    /*
        Small hack: generating a string source code of an "impossible" class in order to
        generate a PsiFile with it, then extract ES6Class from it, then extract MemberInfos.
        Created for top-level functions, but used even if a functions is a method of a class.
     */
    private fun generateMemberInfo(project: Project, methods: List<JSFunction>): Set<JSMemberInfo> {
        val strClazz = buildClassStringFromMethods(methods)
        val abstractPsiFile = PsiFileFactory.getInstance(project)
            .createFileFromText(jsLanguage, strClazz)
        val clazz = PsiTreeUtil.getChildOfType(abstractPsiFile, JSClass::class.java)
        val res = mutableListOf<JSMemberInfo>()
        JSMemberInfo.extractClassMembers(clazz!!, res) { true }
        return res.toSet()
    }
}