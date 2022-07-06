package org.utbot.intellij.plugin.ui.utils


import com.intellij.codeInsight.TestFrameworks
import com.intellij.lang.ecmascript6.psi.ES6Class
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType

class JsPsiElementHandler(
    override val classClass: Class<ES6Class> = ES6Class::class.java,
    override val methodClass: Class<JSFunction> = JSFunction::class.java,
) : PsiElementHandler {
    @Suppress("UNCHECKED_CAST")
    override fun <T> toPsi(element: PsiElement, clazz: Class<T>): T {
        val fac = PsiElementFactory.getInstance(element.project)
        when {
            element is JSFunction -> {
                return fac.createMethod(element.name!!, null) as? T ?: error("Could not cast $element to $clazz")
            }
            element is ES6Class -> {
                return fac.createClass(element.name!!) as? T ?: error("Could not cast $element to $clazz")
            }
            else -> error("Could not cast $element to $clazz")
        }
    }

    override fun isCreateTestActionAvailable(element: PsiElement): Boolean {
        containingClass(element)?.let { psiClass ->
            val psiFile = element.containingFile
            if (psiFile.containingDirectory == null) return false
            if (psiClass.isAnnotationType ||
                psiClass is PsiAnonymousClass
            ) {
                return false
            }
            return TestFrameworks.detectFramework(psiClass) == null
        }
        return false

    }

    override fun containingClass(element: PsiElement) =
        PsiTreeUtil.getParentOfType(element, ES6Class::class.java, false)?.let {
            val fac = PsiElementFactory.getInstance(element.project)
            fac.createClass(it.name!!)
        }
}