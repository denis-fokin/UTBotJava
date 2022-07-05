package org.utbot.intellij.plugin.ui.utils


import com.intellij.lang.ecmascript6.psi.ES6Class
import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testIntegration.TestIntegrationUtils
import com.intellij.testIntegration.createTest.CreateTestAction
import org.jetbrains.uast.toUElement

class JsPsiElementHandler(
    override val classClass: Class<ES6Class> = ES6Class::class.java,
    override val methodClass: Class<JSFunction> = JSFunction::class.java,
): PsiElementHandler {
    /**
     * Makes a transition from Js to UAST and then to Psi.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> toPsi(element: PsiElement, clazz: Class<T>): T {
        return element.toUElement()?.javaPsi as? T ?: error("Could not cast $element to $clazz")
    }

    override fun isCreateTestActionAvailable(element: PsiElement): Boolean =
        CreateTestAction.isAvailableForElement(element)

    override fun containingClass(element: PsiElement) =
        if (PsiTreeUtil.getParentOfType(element, PsiClass::class.java, false) != null) {
            TestIntegrationUtils.findOuterClass(element)
        } else null
}