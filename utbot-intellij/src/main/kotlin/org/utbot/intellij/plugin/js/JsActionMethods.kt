package org.utbot.intellij.plugin.js

import com.intellij.lang.ecmascript6.psi.ES6Class
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

object JsActionMethods {

    const val jsId = "ECMAScript 6"

    fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val (methods, focusedMethod) = getPsiTargets(e) ?: return
    }

    fun update(e: AnActionEvent) {
        e.presentation.isEnabled = getPsiTargets(e) != null
    }

    private fun getPsiTargets(e: AnActionEvent): Pair<Set<JSFunction>, JSFunction?>? {
        e.project ?: return null
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        val file = e.getData(CommonDataKeys.PSI_FILE) as? JSFile ?: return null
        val element = findPsiElement(file, editor) ?: return null
        val focusedMethod = getContainingMethod(element)
        containingClass(element)?.let {
            val methods = it.functions ?: return null
            return methods.toSet() to focusedMethod
        }
        return (file.statements.filterIsInstance<JSFunction>().toSet() to focusedMethod)
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
}